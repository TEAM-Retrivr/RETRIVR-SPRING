package retrivr.retrivrspring.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.*;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.SignupTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.*;
import retrivr.retrivrspring.presentation.admin.auth.res.*;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SignupTokenRepository signupTokenRepository;

    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 1. 이메일로 Organization 조회
        Organization org = organizationRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 계정 상태 확인
        if (org.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new ApplicationException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // 승인 전(PENDING)은 로그인 불가
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.ACCOUNT_NOT_APPROVED);
        }

        if (!passwordEncoder.matches(request.password(), org.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 마지막 로그인 시간 업데이트
        org.updateLastLoginAt(LocalDateTime.now());
        organizationRepository.save(org);

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(org.getId(), org.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(org.getId(), org.getEmail());

        // 6. 응답 반환
        return new AdminLoginResponse(org.getId(), org.getEmail(), accessToken, refreshToken);
    }

    @Transactional
    public AdminSignupResponse signup(AdminSignupRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // 1) signupToken row 조회
        SignupToken token = signupTokenRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND));

        // 2) 코드 검증을 거친 상태인지 확인
        if (token.getCodeVerifiedAt() == null) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        // 3) signupToken 만료
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_EXPIRED);
        }

        // 4) signupToken 1회성
        if (token.getUsedAt() != null) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_ALREADY_USED);
        }

        // 5) signupToken 비교 (현재 tokenHash는 signupTokenHash)
        if (!passwordEncoder.matches(request.signupToken(), token.getTokenHash())) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        // 6) 가입 입력값 검증
        if (request.password() == null || request.password().length() < 8) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        // 3. 중복 검사
        if (organizationRepository.findByEmail(email).isPresent()) {
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }

        // 7) Organization 생성
        String hashedPw = passwordEncoder.encode(request.password());

        String safeName = request.organizationName() == null ? "" : request.organizationName().trim();
        String searchKey = safeName.replaceAll("\\s+", "-") + "-" + System.currentTimeMillis();

        // 6. 엔티티 생성 및 저장
        Organization org = Organization.builder()
                .email(email)
                .passwordHash(hashedPw)
                .name(safeName)
                .status(OrganizationStatus.ACTIVE) // TODO: 가입 승인 프로세스 도입 시 PENDING으로 변경
                .searchKey(searchKey)
                .build();

        try {
            Organization saved = organizationRepository.save(org);

            // 8) 가입 성공 시점에만 signupToken 사용 처리 (재시도 가능성 보장)
            token.markUsed(LocalDateTime.now());

            return new AdminSignupResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getStatus().name()
            );
        } catch (DataIntegrityViolationException e) {
            // DB unique constraint 위반 처리
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }
    }

    @Transactional
    public PasswordResetResponse resetPassword(PasswordResetRequest request) {

        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_POLICY_VIOLATION);
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH);
        }

        Organization organization = organizationRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND)
                );

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(organization)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND)
                );

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        if (token.getUsedAt() != null) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
        }

        if (!passwordEncoder.matches(request.token(), token.getTokenHash())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        organization.changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed(LocalDateTime.now());

        return new PasswordResetResponse(organization.getEmail(), "Password updated successfully");
    }

    @Transactional
    public EmailCodeSendResponse sendSignupEmailCode(EmailVerificationSendRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // 이미 가입된 이메일이면 차단
        if (organizationRepository.findByEmail(email).isPresent()) {
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }

        emailVerificationService.sendCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP));

        return new EmailCodeSendResponse(true, 600, "인증 코드가 이메일로 발송되었습니다.");
    }

    @Transactional
    public EmailCodeVerifyResponse verifySignupEmailCode(EmailVerificationRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        emailVerificationService.verify(
                new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, request.code())
        );

        String rawSignupToken = "st_" + UUID.randomUUID();
        String signupTokenHash = passwordEncoder.encode(rawSignupToken);

        signupTokenRepository.deleteByEmail(email);

        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(signupTokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        token.markCodeVerified(LocalDateTime.now());

        signupTokenRepository.save(token);

        return new EmailCodeVerifyResponse(rawSignupToken, 600);
    }
}