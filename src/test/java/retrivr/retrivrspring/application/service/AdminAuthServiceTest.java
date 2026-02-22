package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.BeforeEach;
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
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.PasswordResetTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private final String email = "admin@retrivr.com";
    private final String rawPassword = "Password123!";
    private final String hashedPassword = "$2a$10$mockhashedpasswordhashhashhash";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("login 성공 시 토큰과 조직 정보 반환")
    void login_success() {
        // given
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

        // when
        var res = adminAuthService.login(new AdminLoginRequest(email, rawPassword));

        // then
        assertNotNull(res);
        assertEquals(org.getId(), res.organizationId());
        assertEquals(org.getEmail(), res.email());
        assertEquals("access-token", res.accessToken());
        assertEquals("refresh-token", res.refreshToken());
        then(organizationRepository).should().save(org); // lastLoginAt update path
    }

    @Test
    @DisplayName("login 실패 - 잘못된 비밀번호")
    void login_invalid_password() {
        // given
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .searchKey("org-1")
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordEncoder.matches("wrong", hashedPassword)).willReturn(false);

        // when / then
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.login(new AdminLoginRequest(email, "wrong")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    @DisplayName("login 실패 - 정지된 계정")
    void login_suspended() {
        // given
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .name("org")
                .status(OrganizationStatus.SUSPENDED)
                .searchKey("org-1")
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));

        // when / then
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.login(new AdminLoginRequest(email, rawPassword)));
        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 성공")
    void signup_success() {
        // given
        String orgName = "Test Org";
        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, orgName);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(hashedPassword);

        // will return saved organization with id
        Organization saved = Organization.builder()
                .id(2L)
                .email(email)
                .passwordHash(hashedPassword)
                .name(orgName)
                .status(OrganizationStatus.ACTIVE)
                .searchKey("test-2")
                .build();

        given(organizationRepository.save(ArgumentMatchers.any(Organization.class))).willReturn(saved);

        // when
        var res = adminAuthService.signup(req);

        // then
        assertNotNull(res);
        assertEquals(saved.getId(), res.orgId());
        assertEquals(saved.getEmail(), res.email());
        assertEquals(saved.getName(), res.organizationName());
    }

    @Test
    @DisplayName("signup 실패 - 중복 이메일")
    void signup_duplicate_email() {
        // given
        String orgName = "Test Org";
        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, orgName);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(Organization.builder().build()));

        // when / then
        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - 비밀번호 정책 미달")
    void signup_password_policy_fail() {
        AdminSignupRequest req = new AdminSignupRequest(email, "short", "Org");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("signup 실패 - DB unique 제약 위반")
    void signup_db_constraint_violation() {
        String orgName = "Test Org";
        AdminSignupRequest req = new AdminSignupRequest(email, rawPassword, orgName);

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(hashedPassword);
        given(organizationRepository.save(ArgumentMatchers.any(Organization.class))).willThrow(new DataIntegrityViolationException("unique"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> adminAuthService.signup(req));
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 성공")
    void resetPassword_success() {
        // given
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.ACTIVE)
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("valid-token")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(null)
                .build();

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        "valid-token",
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordResetTokenRepository.findByTokenHash("valid-token")).willReturn(Optional.of(token));
        given(passwordEncoder.encode("NewPassword123!")).willReturn("encoded-new-password");

        // when
        var response = adminAuthService.resetPassword(request);

        // then
        assertEquals(email, response.email());
        assertEquals("Password updated successfully", response.message());
        then(passwordEncoder).should().encode("NewPassword123!");
    }

    @Test
    @DisplayName("resetPassword 실패 - 비밀번호 확인 불일치")
    void resetPassword_password_mismatch() {

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        "token",
                        "NewPassword123!",
                        "WrongPassword"
                );

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.resetPassword(request));

        assertEquals(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 실패 - 토큰 만료")
    void resetPassword_token_expired() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .build();

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("valid-token")
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

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordResetTokenRepository.findByTokenHash("valid-token"))
                .willReturn(Optional.of(expiredToken));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.resetPassword(request));

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 실패 - 이미 사용된 토큰")
    void resetPassword_token_already_used() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .build();

        PasswordResetToken usedToken = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("valid-token")
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

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordResetTokenRepository.findByTokenHash("valid-token"))
                .willReturn(Optional.of(usedToken));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.resetPassword(request));

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 실패 - 존재하지 않는 계정")
    void resetPassword_account_not_found() {

        PasswordResetRequest request =
                new PasswordResetRequest(
                        email,
                        "token",
                        "NewPassword123!",
                        "NewPassword123!"
                );

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> adminAuthService.resetPassword(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

}

