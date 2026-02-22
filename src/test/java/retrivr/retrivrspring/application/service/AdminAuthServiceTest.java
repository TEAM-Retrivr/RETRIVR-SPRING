package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.SignupTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private SignupTokenRepository signupTokenRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private final String email = "admin@retrivr.com";
    private final String rawPassword = "Password123!";
    private final String hashedPassword = "$2a$10$mockhashedpasswordhashhashhash";

    @Test
    @DisplayName("login 성공 시 토큰과 조직 정보 반환 (ACTIVE)")
    void login_success_active() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .searchKey("org-1")
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordEncoder.matches(rawPassword, hashedPassword)).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(org.getId(), org.getEmail())).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(org.getId(), org.getEmail())).willReturn("refresh-token");

        var res = adminAuthService.login(new AdminLoginRequest(email, rawPassword));

        assertNotNull(res);
        assertEquals(org.getId(), res.organizationId());
        assertEquals(org.getEmail(), res.email());
        assertEquals("access-token", res.accessToken());
        assertEquals("refresh-token", res.refreshToken());

        then(organizationRepository).should().save(org);
    }

    @Test
    @DisplayName("login 실패 - 잘못된 비밀번호")
    void login_invalid_password() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.ACTIVE)
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordEncoder.matches("wrong", hashedPassword)).willReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.login(new AdminLoginRequest(email, "wrong")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    @DisplayName("login 실패 - 정지된 계정")
    void login_suspended() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.SUSPENDED)
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.login(new AdminLoginRequest(email, rawPassword)));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("login 실패 - 승인 대기(PENDING) 계정 (ACTIVE만 로그인 허용인 경우)")
    void login_pending_forbidden() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.PENDING)
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.login(new AdminLoginRequest(email, rawPassword)));

        assertEquals(ErrorCode.ACCOUNT_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 성공 - signupToken 검증 후 PENDING 생성, 가입 성공 시 usedAt 업데이트")
    void signup_success_pending() {
        String orgName = "Test Org";
        String adminCode = "DEV";
        String rawSignupToken = "st_xxx";
        String signupTokenHash = "$2a$10$signupTokenHash";

        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(signupTokenHash) // 이미 verify된 후: tokenHash = signupTokenHash
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        // 정석 버전 전제: code_verified_at != null 이어야 signup 가능
        token.markCodeVerified(LocalDateTime.now());
        // usedAt은 null

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches(rawSignupToken, signupTokenHash)).willReturn(true);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(hashedPassword);

        Organization saved = Organization.builder()
                .id(2L)
                .email(email)
                .passwordHash(hashedPassword)
                .name(orgName)
                .status(OrganizationStatus.PENDING)
                .searchKey("test-2")
                .build();

        given(organizationRepository.save(ArgumentMatchers.any(Organization.class))).willReturn(saved);

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, orgName, adminCode, rawSignupToken);

        var res = adminAuthService.signup(req);

        assertNotNull(res);
        assertEquals(saved.getId(), res.orgId());
        assertEquals(saved.getEmail(), res.email());
        assertEquals(saved.getName(), res.organizationName());
        assertEquals("PENDING", res.status());

        assertNotNull(token.getUsedAt()); // 가입 성공 시점에만 usedAt 찍힘
    }

    @Test
    @DisplayName("signup 실패 - signupToken row 없음")
    void signup_fail_token_not_found() {
        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.empty());

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.SIGNUP_TOKEN_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - 코드 검증 전(code_verified_at null)")
    void signup_fail_code_not_verified() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        // codeVerifiedAt null

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.SIGNUP_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - signupToken 만료")
    void signup_fail_token_expired() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.SIGNUP_TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - signupToken 이미 사용됨(used_at not null)")
    void signup_fail_token_already_used() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());
        token.markUsed(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.SIGNUP_TOKEN_ALREADY_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - signupToken 불일치")
    void signup_fail_token_invalid() {
        String rawSignupToken = "st_xxx";
        String signupTokenHash = "$2a$10$signupTokenHash";

        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(signupTokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches(rawSignupToken, signupTokenHash)).willReturn(false);

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", rawSignupToken);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.SIGNUP_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - 비밀번호 정책 미달")
    void signup_fail_password_policy() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("st_x", "hash")).willReturn(true);

        AdminSignupRequest req = new AdminSignupRequest(email, "short", "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - 중복 이메일(이미 organization 존재)")
    void signup_fail_duplicate_email() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("st_x", "hash")).willReturn(true);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(Organization.builder().build()));

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - DB unique 제약 위반")
    void signup_fail_db_unique() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("st_x", "hash")).willReturn(true);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(hashedPassword);
        given(organizationRepository.save(ArgumentMatchers.any(Organization.class)))
                .willThrow(new DataIntegrityViolationException("unique"));

        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, "Org", "DEV", "st_x");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());

        assertNull(token.getUsedAt()); // save 실패면 usedAt 찍히면 안 됨
    }

    /* ===================== SIGNUP EMAIL SEND ===================== */

    @Test
    @DisplayName("sendSignupEmailCode 성공 - 기존 요청 삭제 후 저장, 응답 600초")
    void sendSignupEmailCode_success() {
        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());

        given(passwordEncoder.encode(ArgumentMatchers.anyString()))
                .willReturn("$2a$10$codehash");

        EmailVerificationSendRequest req = new EmailVerificationSendRequest(email);

        var res = adminAuthService.sendSignupEmailCode(req);

        assertTrue(res.success());
        assertEquals(600, res.expiresInSeconds());
        assertNotNull(res.message());

        then(signupTokenRepository).should().deleteByEmail(email);
        then(signupTokenRepository).should().save(ArgumentMatchers.any(SignupToken.class));
    }

    @Test
    @DisplayName("sendSignupEmailCode 실패 - 이미 가입된 이메일")
    void sendSignupEmailCode_fail_already_exists() {
        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(Organization.builder().build()));

        EmailVerificationSendRequest req = new EmailVerificationSendRequest(email);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.sendSignupEmailCode(req));
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }


    @Test
    @DisplayName("verifySignupEmailCode 성공 - code 검증 후 signupToken 발급, code_verified_at 세팅")
    void verifySignupEmailCode_success() {
        String codeHash = "$2a$10$codehash";
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(codeHash) // codeHash 단계
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("123456", codeHash)).willReturn(true);
        given(passwordEncoder.encode(ArgumentMatchers.anyString()))
                .willReturn("$2a$10$signupTokenHash");

        EmailVerificationRequest req = new EmailVerificationRequest(email, "123456");

        var res = adminAuthService.verifySignupEmailCode(req);

        assertNotNull(res.signupToken());
        assertTrue(res.signupToken().startsWith("st_"));
        assertEquals(600, res.expiresInSeconds());

        assertNotNull(token.getCodeVerifiedAt());
        assertEquals("$2a$10$signupTokenHash", token.getTokenHash());
    }

    @Test
    @DisplayName("verifySignupEmailCode 실패 - 요청 없음")
    void verifySignupEmailCode_fail_not_found() {
        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.empty());

        EmailVerificationRequest req = new EmailVerificationRequest(email, "123456");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.verifySignupEmailCode(req));
        assertEquals(ErrorCode.SIGNUP_EMAIL_CODE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("verifySignupEmailCode 실패 - 만료")
    void verifySignupEmailCode_fail_expired() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("codeHash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));

        EmailVerificationRequest req = new EmailVerificationRequest(email, "123456");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.verifySignupEmailCode(req));
        assertEquals(ErrorCode.SIGNUP_EMAIL_CODE_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("verifySignupEmailCode 실패 - 이미 코드 검증 완료(code_verified_at not null)")
    void verifySignupEmailCode_fail_already_used() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("codeHash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));

        EmailVerificationRequest req = new EmailVerificationRequest(email, "123456");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.verifySignupEmailCode(req));
        assertEquals(ErrorCode.SIGNUP_EMAIL_CODE_ALREADY_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("verifySignupEmailCode 실패 - 코드 불일치")
    void verifySignupEmailCode_fail_mismatch() {
        String codeHash = "$2a$10$codehash";
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(codeHash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("123456", codeHash)).willReturn(false);

        EmailVerificationRequest req = new EmailVerificationRequest(email, "123456");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.verifySignupEmailCode(req));
        assertEquals(ErrorCode.SIGNUP_EMAIL_CODE_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 성공 - 해시 토큰 matches 검증")
    void resetPassword_success() {

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.ACTIVE)
                .build();

        String rawToken = "valid-token";
        String encodedToken = "$2a$10$encodedTokenHash";

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash(encodedToken)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(null)
                .build();

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        rawToken,
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email))
                .willReturn(Optional.of(org));

        given(passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(org))
                .willReturn(Optional.of(token));

        given(passwordEncoder.matches(rawToken, encodedToken))
                .willReturn(true);

        given(passwordEncoder.encode("NewPassword123!"))
                .willReturn("encoded-new-password");

        var response = adminAuthService.resetPassword(request);

        assertEquals(email, response.email());
        assertEquals("Password updated successfully", response.message());

        then(passwordEncoder).should().matches(rawToken, encodedToken);
        then(passwordEncoder).should().encode("NewPassword123!");
    }

    @Test
    @DisplayName("resetPassword 실패 - 토큰 불일치")
    void resetPassword_token_invalid() {

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .build();

        String rawToken = "invalid-token";
        String encodedToken = "$2a$10$encodedTokenHash";

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash(encodedToken)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(null)
                .build();

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        rawToken,
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email))
                .willReturn(Optional.of(org));

        given(passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(org))
                .willReturn(Optional.of(token));

        given(passwordEncoder.matches(rawToken, encodedToken))
                .willReturn(false);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.resetPassword(request)
        );

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, ex.getErrorCode());
    }
    @Test
    @DisplayName("resetPassword 실패 - 토큰 만료")
    void resetPassword_token_expired() {

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("$2a$10$encodedTokenHash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .usedAt(null)
                .build();

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        "valid-token",
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email))
                .willReturn(Optional.of(org));

        given(passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(org))
                .willReturn(Optional.of(token));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.resetPassword(request)
        );

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 실패 - 이미 사용된 토큰")
    void resetPassword_token_already_used() {

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("$2a$10$encodedTokenHash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(LocalDateTime.now())
                .build();

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        "valid-token",
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email))
                .willReturn(Optional.of(org));

        given(passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(org))
                .willReturn(Optional.of(token));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.resetPassword(request)
        );

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 실패 - 존재하지 않는 계정")
    void resetPassword_account_not_found() {
        PasswordResetRequest request = new PasswordResetRequest(
                email, "token", "NewPassword123!", "NewPassword123!"
        );

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.resetPassword(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }
}