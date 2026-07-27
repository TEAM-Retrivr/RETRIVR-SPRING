package retrivr.retrivrspring.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.PasswordVerificationTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordVerificationRequest;

@ExtendWith(MockitoExtension.class)
class PasswordVerificationServiceTest {

    private static final Long ORGANIZATION_ID = 1L;
    private static final String RAW_PASSWORD = "Password123!";
    private static final String PASSWORD_HASH = "encoded-password";
    private static final String TOKEN_HASH = "encoded-token";

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordVerificationTokenRepository passwordVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordVerificationService passwordVerificationService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(ORGANIZATION_ID)
                .email("admin@retrivr.com")
                .passwordHash(PASSWORD_HASH)
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("encoded-admin-code")
                .build();
    }

    @Test
    void verify_issuesPurposeBoundTokenWhenPasswordMatches() {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(true);
        given(passwordEncoder.encode(any(String.class))).willReturn(TOKEN_HASH);

        var response = passwordVerificationService.verify(
                ORGANIZATION_ID,
                new AdminPasswordVerificationRequest(
                        RAW_PASSWORD,
                        PasswordVerificationPurpose.EMAIL_CHANGE
                )
        );

        assertNotNull(response.verificationToken());
        assertEquals(true, response.verificationToken().startsWith("pvt_"));
        assertEquals(300, response.expiresIn());
        verify(passwordVerificationTokenRepository).deleteByOrganizationAndPurpose(
                organization,
                PasswordVerificationPurpose.EMAIL_CHANGE
        );

        ArgumentCaptor<PasswordVerificationToken> captor =
                ArgumentCaptor.forClass(PasswordVerificationToken.class);
        verify(passwordVerificationTokenRepository).save(captor.capture());
        assertEquals(PasswordVerificationPurpose.EMAIL_CHANGE, captor.getValue().getPurpose());
        assertEquals(TOKEN_HASH, captor.getValue().getTokenHash());
    }

    @Test
    void verify_rejectsIncorrectPassword() {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(false);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> passwordVerificationService.verify(
                        ORGANIZATION_ID,
                        new AdminPasswordVerificationRequest(
                                RAW_PASSWORD,
                                PasswordVerificationPurpose.PASSWORD_CHANGE
                        )
                )
        );

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void validate_rejectsTokenForDifferentPurpose() {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
        given(passwordVerificationTokenRepository
                .findTopByOrganizationAndPurposeOrderByCreatedAtDesc(
                        organization,
                        PasswordVerificationPurpose.ADMIN_CODE_CHANGE
                ))
                .willReturn(Optional.empty());

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> passwordVerificationService.validate(
                        ORGANIZATION_ID,
                        PasswordVerificationPurpose.ADMIN_CODE_CHANGE,
                        "pvt_token-for-email-change"
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    void validate_rejectsExpiredToken() {
        PasswordVerificationToken token = tokenExpiringAt(LocalDateTime.now().minusSeconds(1));
        givenToken(PasswordVerificationPurpose.PASSWORD_CHANGE, token);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> passwordVerificationService.validate(
                        ORGANIZATION_ID,
                        PasswordVerificationPurpose.PASSWORD_CHANGE,
                        "pvt_expired"
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED,
                exception.getErrorCode()
        );
    }

    @Test
    void validateAndConsume_marksValidTokenAsUsed() {
        PasswordVerificationToken token = tokenExpiringAt(LocalDateTime.now().plusMinutes(5));
        givenToken(PasswordVerificationPurpose.PASSWORD_CHANGE, token);
        given(passwordEncoder.matches("pvt_valid", TOKEN_HASH)).willReturn(true);

        passwordVerificationService.validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.PASSWORD_CHANGE,
                "pvt_valid"
        );

        assertNotNull(token.getUsedAt());
    }

    @Test
    void validate_rejectsAlreadyUsedToken() {
        PasswordVerificationToken token = tokenExpiringAt(LocalDateTime.now().plusMinutes(5));
        token.markUsed(LocalDateTime.now());
        givenToken(PasswordVerificationPurpose.EMAIL_CHANGE, token);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> passwordVerificationService.validate(
                        ORGANIZATION_ID,
                        PasswordVerificationPurpose.EMAIL_CHANGE,
                        "pvt_used"
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED,
                exception.getErrorCode()
        );
    }

    @Test
    void validate_rejectsInvalidTokenValue() {
        PasswordVerificationToken token = tokenExpiringAt(LocalDateTime.now().plusMinutes(5));
        givenToken(PasswordVerificationPurpose.EMAIL_CHANGE, token);
        given(passwordEncoder.matches("pvt_wrong", TOKEN_HASH)).willReturn(false);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> passwordVerificationService.validate(
                        ORGANIZATION_ID,
                        PasswordVerificationPurpose.EMAIL_CHANGE,
                        "pvt_wrong"
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID,
                exception.getErrorCode()
        );
    }

    private PasswordVerificationToken tokenExpiringAt(LocalDateTime expiresAt) {
        return PasswordVerificationToken.builder()
                .organization(organization)
                .purpose(PasswordVerificationPurpose.PASSWORD_CHANGE)
                .tokenHash(TOKEN_HASH)
                .expiresAt(expiresAt)
                .build();
    }

    private void givenToken(
            PasswordVerificationPurpose purpose,
            PasswordVerificationToken token
    ) {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
        given(passwordVerificationTokenRepository
                .findTopByOrganizationAndPurposeOrderByCreatedAtDesc(organization, purpose))
                .willReturn(Optional.of(token));
    }
}
