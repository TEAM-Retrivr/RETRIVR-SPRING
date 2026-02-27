package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.repository.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.SignupTokenRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SignupTokenRepository signupTokenRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private final String email = "test@test.com";

    @Test
    void sendCode_success_create() {

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-code");

        emailVerificationService.sendCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
        );

        verify(emailVerificationRepository, times(1))
                .save(any(EmailVerification.class));
    }

    @Test
    void sendCode_resendBlocked() {

        EmailVerification existing = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        ReflectionTestUtils.setField(
                existing,
                "updatedAt",
                LocalDateTime.now()
        );

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(existing));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.sendCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
                ));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS, ex.getErrorCode());
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

        when(passwordEncoder.matches("123456", "hashed"))
                .thenReturn(true);

        when(passwordEncoder.encode(any()))
                .thenReturn("signupTokenHash");

        var response = emailVerificationService.verify(
                new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
        );

        // 반환 타입 확인
        assertTrue(response instanceof EmailCodeVerifyResponse);

        EmailCodeVerifyResponse tokenResponse = (EmailCodeVerifyResponse) response;

        assertNotNull(tokenResponse.signupToken());
        assertTrue(tokenResponse.signupToken().startsWith("st_"));
        assertEquals(600, tokenResponse.expiresInSeconds());

        // DB 저장 확인
        verify(signupTokenRepository, times(1))
                .save(any(SignupToken.class));
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

        when(passwordEncoder.matches("123456", "hashed"))
                .thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
                ));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void verify_expired() {

        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().minusMinutes(1)
        );

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(verification));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
                ));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_EXPIRED, ex.getErrorCode());
    }

    @Test
    void verify_alreadyVerified() {

        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        verification.markVerified(LocalDateTime.now());

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(verification));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.SIGNUP, "123456")
                ));

        assertEquals(ErrorCode.EMAIL_ALREADY_VERIFIED, ex.getErrorCode());
    }
}