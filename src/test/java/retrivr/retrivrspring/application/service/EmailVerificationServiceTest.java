package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationCodeSender;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.RefreshTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.properties.EmailVerificationProperties;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminEmailChangeResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

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
import static org.mockito.Mockito.doThrow;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationCodeSender emailVerificationCodeSender;

    @Mock
    private EmailVerificationProperties emailVerificationProperties;

    @Mock
    private PasswordVerificationService passwordVerificationService;

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

        emailVerificationService.sendPublicCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
        );

        verify(emailVerificationRepository, times(1)).saveAndFlush(any(EmailVerification.class));
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
        // 40초 전에 발송된 상태 → 60초 정책이면 약 20초 남아야 한다.
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.now().minusSeconds(40));

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(existing));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.sendPublicCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
                )
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS, ex.getErrorCode());
        // 클라이언트가 카운트다운을 복원할 수 있도록 남은 초가 detail 로 전달되어야 한다.
        long remaining = Long.parseLong(ex.getDetail());
        assertTrue(remaining > 15 && remaining <= 20, "remaining=" + remaining);
        verifyNoInteractions(emailVerificationCodeSender);
    }

    @Test
    void sendCode_allowsResendAfterBlockWindow() {
        EmailVerification existing = EmailVerification.create(
                email,
                EmailVerificationPurpose.SIGNUP,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );
        // 61초 전 발송 → 재발송 가능
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.now().minusSeconds(61));

        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hashed-code");

        emailVerificationService.sendPublicCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
        );

        verify(emailVerificationCodeSender, times(1))
                .sendVerificationCode(eq(email), anyString(), eq(EmailVerificationPurpose.SIGNUP), eq(600));
    }

    @Test
    void sendCode_responseCarriesResendAndExpiryWindows() {
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        EmailVerificationSendResponse response = emailVerificationService.sendPublicCode(
                new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
        );

        // 프론트가 상수를 하드코딩하지 않도록 두 정책값을 모두 응답으로 내려준다.
        assertEquals(600, response.expiresInSeconds());
        assertEquals(60, response.resendAvailableInSeconds());
    }

    @Test
    void sendPublicCode_rejectsEmailChangePurpose() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.sendPublicCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.EMAIL_CHANGE)
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository, emailVerificationCodeSender, organizationRepository);
    }

    @Test
    void verify_rejectsEmailChangePurpose_withoutConsumingCode() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest(email, EmailVerificationPurpose.EMAIL_CHANGE, "123456")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
        // 인증 코드가 조회조차 되지 않아야 markVerified 로 소모되지 않는다.
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void sendChangeEmailCode_rejectsSameAsCurrentEmail() {
        Long organizationId = 1L;
        // 대소문자만 다른 현재 이메일도 동일한 것으로 판정되어야 한다.
        Organization organization = organization(organizationId, "Test@TEST.com");

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> emailVerificationService.sendChangeEmailCode(
                        adminSendRequest(),
                        organizationId
                )
        );

        assertEquals(ErrorCode.EMAIL_SAME_AS_CURRENT, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository, emailVerificationCodeSender);
    }

    @Test
    void sendChangeEmailCode_rejectsEmailUsedByAnotherOrganization() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, "old@test.com");

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.of(organization(2L, email)));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> emailVerificationService.sendChangeEmailCode(
                        adminSendRequest(),
                        organizationId
                )
        );

        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository, emailVerificationCodeSender);
    }

    @Test
    void sendChangeEmailCode_success_sendsCode() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, "old@test.com");

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.EMAIL_CHANGE))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        emailVerificationService.sendChangeEmailCode(
                adminSendRequest(),
                organizationId
        );

        verify(emailVerificationRepository, times(1)).saveAndFlush(any(EmailVerification.class));
        verify(emailVerificationCodeSender, times(1))
                .sendVerificationCode(eq(email), anyString(), eq(EmailVerificationPurpose.EMAIL_CHANGE), eq(600));
        verify(passwordVerificationService).validate(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                "pvt_email"
        );
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
    void verifyChangeEmail_success_appliesEmailImmediately() {
        Long organizationId = 1L;
        Organization organization = Organization.builder()
                .id(organizationId)
                .email("old@test.com")
                .passwordHash("pw")
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("code")
                .build();

        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.EMAIL_CHANGE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        AdminEmailChangeResponse response = emailVerificationService.verifyChangeEmail(
                adminVerifyRequest(),
                organizationId
        );

        assertEquals(organizationId, response.organizationId());
        assertEquals(email, response.email());
        assertEquals(email, organization.getEmail());
        assertTrue(verification.isVerified());
        // 이메일 변경 시 refresh token 은 '변경 전' 이메일로 저장되어 있으므로, 그 값으로 폐기되어야 한다.
        verify(refreshTokenRepository, times(1)).deleteAllByEmail("old@test.com");
        verify(passwordVerificationService).validateAndConsume(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                "pvt_email"
        );
        verify(organizationRepository, times(1)).saveAndFlush(organization);
        verifyNoInteractions(signupTokenRepository, passwordResetTokenRepository);
    }

    @Test
    void verifyChangeEmail_codeMismatchDoesNotConsumePasswordVerificationToken() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, "old@test.com");
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(emailVerificationRepository.findByEmailAndPurpose(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE
        )).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(false);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        adminVerifyRequest(),
                        organizationId
                )
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, exception.getErrorCode());
        verify(passwordVerificationService).validate(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                "pvt_email"
        );
        verify(passwordVerificationService, times(0)).validateAndConsume(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                "pvt_email"
        );
    }

    @Test
    void verifyChangeEmail_tokenConsumptionFailureDoesNotVerifyEmailCode() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, "old@test.com");
        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(emailVerificationRepository.findByEmailAndPurpose(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE
        )).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        doThrow(new ApplicationException(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
        )).when(passwordVerificationService).validateAndConsume(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                "pvt_email"
        );

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        adminVerifyRequest(),
                        organizationId
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED,
                exception.getErrorCode()
        );
        assertEquals(false, verification.isVerified());
        assertEquals("old@test.com", organization.getEmail());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void verifyChangeEmail_duplicateEmailAtCommit_translatedToAlreadyExist() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, "old@test.com");

        EmailVerification verification = EmailVerification.create(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                "hashed",
                LocalDateTime.now().plusMinutes(10)
        );

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.EMAIL_CHANGE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        // 동시 요청이 먼저 커밋되어 unique 제약에 걸리는 상황
        when(organizationRepository.saveAndFlush(organization))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        adminVerifyRequest(),
                        organizationId
                )
        );

        // 500 이 아니라 400 으로 내려가야 한다.
        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
    }

    @Test
    void sendCode_duplicateAtFlush_translatedToTooManyRequests() {
        when(emailVerificationRepository.findByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
        when(emailVerificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate (email, purpose)"));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> emailVerificationService.sendPublicCode(
                        new EmailVerificationSendRequest(email, EmailVerificationPurpose.SIGNUP)
                )
        );

        assertEquals(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS, ex.getErrorCode());
        verifyNoInteractions(emailVerificationCodeSender);
    }

    @Test
    void verifyChangeEmail_duplicateEmail_throws() {
        Long organizationId = 1L;
        Organization organization = Organization.builder()
                .id(organizationId)
                .email("old@test.com")
                .passwordHash("pw")
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("code")
                .build();

        Organization anotherOrg = Organization.builder()
                .id(2L)
                .email(email)
                .passwordHash("pw")
                .name("other")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("code")
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.of(anotherOrg));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        adminVerifyRequest(),
                        organizationId
                )
        );

        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
        assertEquals("old@test.com", organization.getEmail());
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void verifyChangeEmail_sameAsCurrentEmail_throws() {
        Long organizationId = 1L;
        Organization organization = organization(organizationId, email);

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> emailVerificationService.verifyChangeEmail(
                        adminVerifyRequest(),
                        organizationId
                )
        );

        assertEquals(ErrorCode.EMAIL_SAME_AS_CURRENT, ex.getErrorCode());
        verifyNoInteractions(emailVerificationRepository);
    }

    private Organization organization(Long id, String email) {
        return Organization.builder()
                .id(id)
                .email(email)
                .passwordHash("pw")
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("code")
                .build();
    }

    private AdminEmailVerificationSendRequest adminSendRequest() {
        return new AdminEmailVerificationSendRequest(email, "pvt_email");
    }

    private AdminEmailVerificationRequest adminVerifyRequest() {
        return new AdminEmailVerificationRequest(email, "123456", "pvt_email");
    }
}
