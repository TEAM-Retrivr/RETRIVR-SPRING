package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private SignupTokenRepository signupTokenRepository;
    @Mock private EmailVerificationService emailVerificationService; // ★ 추가

    @InjectMocks
    private AdminAuthService adminAuthService;

    private final String email = "admin@retrivr.com";
    private final String rawPassword = "Password123!";
    private final String hashedPassword = "$2a$10$mockhashedpasswordhashhashhash";

    @Test
    @DisplayName("login 성공")
    void login_success() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.ACTIVE)
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordEncoder.matches(rawPassword, hashedPassword)).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(1L, email)).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(1L, email)).willReturn("refresh");

        var res = adminAuthService.login(new AdminLoginRequest(email, rawPassword));

        assertEquals("access", res.accessToken());
        assertEquals("refresh", res.refreshToken());
    }

    @Test
    @DisplayName("signup 성공")
    void signup_success() {

        String rawSignupToken = "st_xxx";
        String hashedSignupToken = "$2a$10$signupHash";

        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash(hashedSignupToken)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches(rawSignupToken, hashedSignupToken)).willReturn(true);
        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(hashedPassword);

        Organization saved = Organization.builder()
                .id(2L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.PENDING)
                .build();

        given(organizationRepository.save(any())).willReturn(saved);

        var res = adminAuthService.signup(
                new AdminSignupRequest(email, rawPassword, "Org", "DEV", rawSignupToken)
        );

        assertEquals(2L, res.orgId());
        assertNotNull(token.getUsedAt());
    }

    @Test
    @DisplayName("sendSignupEmailCode 성공 - EmailVerificationService 호출")
    void sendSignupEmailCode_success() {

        given(organizationRepository.findByEmail(email)).willReturn(Optional.empty());

        EmailVerificationSendRequest req =
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP);

        var res = adminAuthService.sendSignupEmailCode(req);

        assertTrue(res.success());
        then(emailVerificationService)
                .should()
                .sendCode(any(EmailVerificationSendRequest.class));
    }

    @Test
    @DisplayName("sendSignupEmailCode 실패 - 이미 가입된 이메일")
    void sendSignupEmailCode_fail() {
        given(organizationRepository.findByEmail(email))
                .willReturn(Optional.of(Organization.builder().build()));

        EmailVerificationSendRequest req =
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.sendSignupEmailCode(req)
        );

        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("verifySignupEmailCode 성공 - EmailVerification 검증 후 SignupToken 발급")
    void verifySignupEmailCode_success() {

        when(emailVerificationService.verify(any()))
                .thenReturn(new EmailCodeVerifyResponse("st_test", 600));

        given(passwordEncoder.encode(any()))
                .willReturn("signupTokenHash");

        var res = adminAuthService.verifySignupEmailCode(
                new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
        );

        assertNotNull(res.signupToken());
    }

    @Test
    @DisplayName("verifySignupEmailCode 실패 - 이메일 인증 검증 실패")
    void verifySignupEmailCode_fail() {

        willThrow(new ApplicationException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH))
                .given(emailVerificationService)
                .verify(any());

        EmailVerificationRequest req =
                new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456");

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.verifySignupEmailCode(req)
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword 성공")
    void resetPassword_success() {

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .organization(org)
                .tokenHash("$2a$10$hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        given(organizationRepository.findByEmail(email)).willReturn(Optional.of(org));
        given(passwordResetTokenRepository
                .findTopByOrganizationOrderByCreatedAtDesc(org))
                .willReturn(Optional.of(token));
        given(passwordEncoder.matches("token", "$2a$10$hash")).willReturn(true);
        given(passwordEncoder.encode("NewPassword123!")).willReturn("encoded");

        var res = adminAuthService.resetPassword(
                new PasswordResetRequest(email, "token", "NewPassword123!", "NewPassword123!")
        );

        assertEquals(email, res.email());
    }
}