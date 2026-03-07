package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.auth.AdminAuthService;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.SignupTokenRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    @DisplayName("login success")
    void login_success() {
        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("encoded-admin-code")
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
    @DisplayName("signup success")
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
        given(passwordEncoder.encode("DEV")).willReturn("encoded-admin-code");

        Organization saved = Organization.builder()
                .id(2L)
                .email(email)
                .passwordHash(hashedPassword)
                .status(OrganizationStatus.PENDING)
                .adminCodeHash("encoded-admin-code")
                .build();

        given(organizationRepository.save(any())).willReturn(saved);

        var res = adminAuthService.signup(
                new AdminSignupRequest(email, rawPassword, "Org", "DEV", rawSignupToken)
        );

        assertEquals(2L, res.orgId());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void signup_blankAdminCode_throwsInvalidValue() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("$2a$10$signupHash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("st_xxx", "$2a$10$signupHash")).willReturn(true);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.signup(
                        new AdminSignupRequest(email, rawPassword, "Org", "   ", "st_xxx")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
        verify(passwordEncoder, never()).encode(rawPassword);
    }

    @Test
    void signup_invalidPasswordPolicy_throwsInvalidValue() {
        SignupToken token = SignupToken.builder()
                .email(email)
                .tokenHash("$2a$10$signupHash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.markCodeVerified(LocalDateTime.now());

        given(signupTokenRepository.findByEmail(email)).willReturn(Optional.of(token));
        given(passwordEncoder.matches("st_xxx", "$2a$10$signupHash")).willReturn(true);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.signup(
                        new AdminSignupRequest(email, "alllowercase1", "Org", "DEV", "st_xxx")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("resetPassword success")
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
                new PasswordResetRequest(email, EmailVerificationPurpose.PASSWORD_RESET, "token", "NewPassword123!", "NewPassword123!")
        );

        assertEquals(email, res.email());
    }

    @Test
    void resetPassword_withInvalidPurpose_throwsInvalidValue() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.resetPassword(
                        new PasswordResetRequest(email, EmailVerificationPurpose.SIGNUP, "token", "NewPassword123!", "NewPassword123!")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
    }

    @Test
    void resetPassword_policyViolation_throwsPolicyViolation() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminAuthService.resetPassword(
                        new PasswordResetRequest(email, EmailVerificationPurpose.PASSWORD_RESET, "token", "NewPassword123", "NewPassword123")
                )
        );

        assertEquals(ErrorCode.PASSWORD_RESET_POLICY_VIOLATION, ex.getErrorCode());
    }
}
