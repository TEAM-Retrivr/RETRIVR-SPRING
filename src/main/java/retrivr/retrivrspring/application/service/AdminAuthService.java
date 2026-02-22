package retrivr.retrivrspring.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.PasswordResetTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.PasswordResetResponse;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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
        // 1. normalize email
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // 2. 비밀번호 기본 정책(최소 길이) 방어
        if (request.password() == null || request.password().length() < 8) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        // 3. 중복 검사
        if (organizationRepository.findByEmail(email).isPresent()) {
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }

        // 7) Organization 생성 (PENDING)
        String hashedPw = passwordEncoder.encode(request.password());

        String safeName = request.organizationName() == null ? "" : request.organizationName().trim();
        String searchKey = safeName.replaceAll("\\s+", "-") + "-" + System.currentTimeMillis();

        // 6. 엔티티 생성 및 저장
        Organization org = Organization.builder()
                .email(email)
                .passwordHash(hashedPw)
                .name(safeName)
                .status(OrganizationStatus.PENDING)
                .searchKey(searchKey)
                .build();

        try {
            Organization saved = organizationRepository.save(org);
            return new AdminSignupResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getStatus().name());
        } catch (DataIntegrityViolationException e) {
            // DB unique constraint 위반 처리
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }
    }

    @Transactional
    public PasswordResetResponse resetPassword(PasswordResetRequest request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH);
        }

        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_POLICY_VIOLATION);
        }

        Organization organization = organizationRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND)
                );

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(request.token())
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND)
                );

        if (!token.getOrganization().getId().equals(organization.getId())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        if (token.getUsedAt() != null) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        organization.changePassword(encodedPassword);

        token.markUsed(LocalDateTime.now());

        return new PasswordResetResponse(
                organization.getEmail(),
                "Password updated successfully"
        );
    }

}