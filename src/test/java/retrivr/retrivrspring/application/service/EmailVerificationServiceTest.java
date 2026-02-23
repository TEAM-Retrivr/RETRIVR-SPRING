package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = mock(Organization.class);
    }

    @Test
    void sendCode_success() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(organization));

        when(emailVerificationRepository.findTopByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Optional.empty());

        emailVerificationService.sendCode("test@test.com");

        verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
    }

    @Test
    void sendCode_emailNotFound() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.sendCode("test@test.com"));

        assertEquals(ErrorCode.EMAIL_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void verify_success() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(organization));

        EmailVerification verification = EmailVerification.create(
                organization,
                "123456",
                LocalDateTime.now().plusMinutes(10)
        );

        when(emailVerificationRepository.findTopByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Optional.of(verification));

        var response = emailVerificationService.verify(
                new EmailVerificationRequest("test@test.com", "123456")
        );

        assertTrue(response.verified());
        assertNotNull(response.verifiedAt());
    }

    @Test
    void verify_codeMismatch() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(organization));

        EmailVerification verification = EmailVerification.create(
                organization,
                "111111",
                LocalDateTime.now().plusMinutes(10)
        );

        when(emailVerificationRepository.findTopByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Optional.of(verification));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest("test@test.com", "123456")
                ));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void verify_expired() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(organization));

        EmailVerification verification = EmailVerification.create(
                organization,
                "123456",
                LocalDateTime.now().minusMinutes(1)
        );

        when(emailVerificationRepository.findTopByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Optional.of(verification));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest("test@test.com", "123456")
                ));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_EXPIRED, ex.getErrorCode());
    }

    @Test
    void verify_alreadyVerified() {
        when(organizationRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(organization));

        EmailVerification verification = EmailVerification.create(
                organization,
                "123456",
                LocalDateTime.now().plusMinutes(10)
        );

        verification.markVerified(LocalDateTime.now());

        when(emailVerificationRepository.findTopByOrganizationOrderByCreatedAtDesc(organization))
                .thenReturn(Optional.of(verification));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> emailVerificationService.verify(
                        new EmailVerificationRequest("test@test.com", "123456")
                ));

        assertEquals(ErrorCode.EMAIL_ALREADY_VERIFIED, ex.getErrorCode());
    }
}