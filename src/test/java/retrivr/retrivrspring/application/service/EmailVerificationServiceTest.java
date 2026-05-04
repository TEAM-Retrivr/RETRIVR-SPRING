package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationCodeSender;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.auth.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.properties.EmailVerificationProperties;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SignupTokenRepository signupTokenRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationCodeSender emailVerificationCodeSender;

    @Mock
    private EmailVerificationProperties emailVerificationProperties;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private final String email = "test@test.com";

    @BeforeEach
    void setUpPolicy() {
        lenient().when(emailVerificationProperties.getExpiresSeconds()).thenReturn(600);
        lenient().when(emailVerificationProperties.getResendBlockSeconds()).thenReturn(60L);
        lenient().when(emailVerificationProperties.getMaxFailedAttempts()).thenReturn(5);
    }

    @Test
    void sendCode_success_create() {
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        emailVerificationService.sendCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
        );

        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
        verify(emailVerificationCodeSender, times(1))
                .sendVerificationCode(eq(email), anyString(), eq(EmailVerificationPurpose.SIGNUP), eq(600));
    }

    @Test
    void sendCode_resendBlocked() {
        EmailVerification existing = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.now());

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(existing));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.sendCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
                )
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS, ex.getErrorCode());
    }

    @Test
    void sendChangeEmailCode_rejectsNonEmailChangePurpose() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.sendChangeEmailCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository, emailVerificationCodeSender);
    }

    @Test
    void verify_success_signup_generates_token() {
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("signupTokenHash");

        EmailCodeVerifyTokenResponse tokenResponse = emailVerificationService.verify(
                new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
        );

        assertEquals("SIGNUP", tokenResponse.tokenType());
        assertNotNull(tokenResponse.token());
        assertTrue(tokenResponse.token().startsWith("st_"));
        assertEquals(600, tokenResponse.expiresInSeconds());

        verify(signupTokenRepository, times(1)).deleteByEmail(email);
        verify(signupTokenRepository, times(1)).save(any());
    }

    @Test
    void verify_codeMismatch() {
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(false);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
                )
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, ex.getErrorCode());
        assertEquals(1, ReflectionTestUtils.getField(verification, "failedAttempts"));
        verify(emailVerificationRepository, times(1)).save(verification);
    }

    @Test
    void verify_success_passwordReset_generates_token() {
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.PASSWORD_RESET,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        Organization org = Organization.builder()
                .id(1L)
                .email(email)
                .passwordHash("encoded-password")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("encoded-admin-code")
                .build();

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.of(org));
        when(passwordEncoder.encode(any())).thenReturn("passwordResetTokenHash");

        EmailCodeVerifyTokenResponse tokenResponse = emailVerificationService.verify(
                new EmailVerificationRequest(email, EmailVerificationPurpose.PASSWORD_RESET, "123456")
        );

        assertEquals("PASSWORD_RESET", tokenResponse.tokenType());
        assertNotNull(tokenResponse.token());
        assertTrue(tokenResponse.token().startsWith("prt_"));
        assertEquals(600, tokenResponse.expiresInSeconds());

        verify(passwordResetTokenRepository, times(1)).deleteByOrganization(org);
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    void verifyChangeEmail_success_generates_token() {
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.EMAIL_CHANGE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        EmailCodeVerifyTokenResponse tokenResponse = emailVerificationService.verifyChangeEmail(
                new EmailVerificationRequest(email, EmailVerificationPurpose.EMAIL_CHANGE, "123456")
        );

        assertEquals("EMAIL_CHANGE", tokenResponse.tokenType());
        assertNotNull(tokenResponse.token());
        assertTrue(tokenResponse.token().startsWith("ect_"));
        assertEquals(600, tokenResponse.expiresInSeconds());
        verifyNoInteractions(signupTokenRepository, passwordResetTokenRepository);
    }

    @Test
    void verifyChangeEmail_rejectsNonEmailChangePurpose() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository);
    }
}
