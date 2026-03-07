package retrivr.retrivrspring.application.service.admin.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.PasswordResetSuccessResponse;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private static final Pattern PASSWORD_POLICY_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$"
    );

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SignupTokenRepository signupTokenRepository;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 1. 이메일로 Organization 조회
        Organization org = organizationRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 계정 상태 확인
        org.assertLoginAllowed();

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
        LocalDateTime now = LocalDateTime.now();

        // 1) signupToken row 조회
        SignupToken token = signupTokenRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SIGNUP_TOKEN_NOT_FOUND));

        // 3) signupToken 검증
        token.assertUsable(request.signupToken(), passwordEncoder, now);

        // 6) 가입 입력값 검증
        if (!isValidPassword(request.password())) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        String adminCode = request.adminCode();
        if (adminCode == null || adminCode.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }
        String trimmedAdminCode = adminCode.trim();

        // 3. 중복 검사
        if (organizationRepository.findByEmail(email).isPresent()) {
            throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }

        // 7) Organization 생성
        String hashedPw = passwordEncoder.encode(request.password());
        String encodedAdminCode = passwordEncoder.encode(trimmedAdminCode);

        String safeName = request.organizationName() == null ? "" : request.organizationName().trim();
        String searchKey = safeName.replaceAll("\\s+", "-") + "-" + System.currentTimeMillis();

        // 6. 엔티티 생성 및 저장
        Organization org = Organization.builder()
                .email(email)
                .passwordHash(hashedPw)
                .name(safeName)
                .status(OrganizationStatus.ACTIVE) // TODO: 가입 승인 프로세스 도입 시 PENDING으로 변경
                .adminCodeHash(encodedAdminCode)
                .searchKey(searchKey)
                .build();

        try {
            Organization saved = organizationRepository.save(org);

            // 8) 가입 성공 시점에만 signupToken 사용 처리 (재시도 가능성 보장)
            token.markUsed(now);

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
    public PasswordResetSuccessResponse resetPassword(PasswordResetRequest request) {
        if (request.purpose() != EmailVerificationPurpose.PASSWORD_RESET) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        if (!isValidPassword(request.newPassword())) {
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

        return PasswordResetSuccessResponse.ok();
    }

    private boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_POLICY_PATTERN.matcher(password).matches();
    }

}
