package retrivr.retrivrspring.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.application.port.image.ImageStoragePort;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.RefreshTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminCodeUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileResponse;

@ExtendWith(MockitoExtension.class)
class AdminProfileServiceTest {

    private static final Long ORGANIZATION_ID = 1L;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordVerificationService passwordVerificationService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ImageStoragePort imageStoragePort;

    @InjectMocks
    private AdminProfileService adminProfileService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(ORGANIZATION_ID)
                .email("old@retrivr.com")
                .passwordHash("old-password")
                .name("old")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("old-code")
                .build();
    }

    @Test
    void getProfile_returnsOrganizationInformationWithoutImageUrlWhenImageDoesNotExist() {
        givenOrganization();

        AdminProfileResponse response = adminProfileService.getProfile(ORGANIZATION_ID);

        assertEquals("old", response.organizationName());
        assertEquals(ORGANIZATION_ID, response.organizationId());
        assertEquals("old@retrivr.com", response.email());
        assertEquals(null, response.profileImageUrl());
        verifyNoInteractions(imageStoragePort);
    }

    @Test
    void getProfile_returnsPresignedUrlWhenProfileImageExists() {
        organization.updateProfileImageKey("organizations/1/profile/image.png");
        givenOrganization();
        given(imageStoragePort.createPresignedDownloadUrl(
                "organizations/1/profile/image.png"
        )).willReturn("https://s3.retrivr/profile-image");

        AdminProfileResponse response = adminProfileService.getProfile(ORGANIZATION_ID);

        assertEquals("https://s3.retrivr/profile-image", response.profileImageUrl());
        verify(imageStoragePort).createPresignedDownloadUrl(
                "organizations/1/profile/image.png"
        );
    }

    @Test
    void updateProfile_changesOnlyOrganizationName() {
        givenOrganization();

        adminProfileService.updateProfile(
                ORGANIZATION_ID,
                new AdminProfileUpdateRequest(" New Org ")
        );

        assertEquals("New Org", organization.getName());
        assertEquals("old-password", organization.getPasswordHash());
        assertEquals("old-code", organization.getAdminCodeHash());
        verifyNoInteractions(passwordEncoder, passwordVerificationService);
    }

    @Test
    void updateProfile_rejectsBlankOrganizationName() {
        givenOrganization();

        DomainException exception = assertThrows(
                DomainException.class,
                () -> adminProfileService.updateProfile(
                        ORGANIZATION_ID,
                        new AdminProfileUpdateRequest("   ")
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, exception.getErrorCode());
        assertEquals("old", organization.getName());
    }

    @Test
    void updateProfile_rejectsOrganizationNameLongerThan255Characters() {
        givenOrganization();

        DomainException exception = assertThrows(
                DomainException.class,
                () -> adminProfileService.updateProfile(
                        ORGANIZATION_ID,
                        new AdminProfileUpdateRequest("a".repeat(256))
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, exception.getErrorCode());
        assertEquals("old", organization.getName());
    }

    @Test
    void updatePassword_changesPasswordAfterConsumingPurposeBoundToken() {
        givenOrganization();
        given(passwordEncoder.encode("NewPassword123!")).willReturn("new-password-hash");

        adminProfileService.updatePassword(
                ORGANIZATION_ID,
                new AdminPasswordUpdateRequest(
                        "NewPassword123!",
                        "NewPassword123!",
                        "pvt_password"
                )
        );

        assertEquals("new-password-hash", organization.getPasswordHash());
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.PASSWORD_CHANGE,
                "pvt_password"
        );
        verify(refreshTokenRepository).deleteAllByEmail("old@retrivr.com");
    }

    @Test
    void updatePassword_rejectsConfirmationMismatch() {
        givenOrganization();

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> adminProfileService.updatePassword(
                        ORGANIZATION_ID,
                        new AdminPasswordUpdateRequest(
                                "NewPassword123!",
                                "DifferentPassword123!",
                                "pvt_password"
                        )
                )
        );

        assertEquals(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH, exception.getErrorCode());
        verifyNoInteractions(passwordVerificationService, refreshTokenRepository);
    }

    @Test
    void updatePassword_rejectsPasswordThatViolatesPolicy() {
        givenOrganization();

        DomainException exception = assertThrows(
                DomainException.class,
                () -> adminProfileService.updatePassword(
                        ORGANIZATION_ID,
                        new AdminPasswordUpdateRequest(
                                "NewPassword123?",
                                "NewPassword123?",
                                "pvt_password"
                        )
                )
        );

        assertEquals(ErrorCode.PASSWORD_RESET_POLICY_VIOLATION, exception.getErrorCode());
        verifyNoInteractions(passwordVerificationService, refreshTokenRepository);
    }

    @Test
    void updateAdminCode_changesCodeAfterConsumingPurposeBoundToken() {
        givenOrganization();
        given(passwordEncoder.encode("123456")).willReturn("new-admin-code-hash");

        adminProfileService.updateAdminCode(
                ORGANIZATION_ID,
                new AdminCodeUpdateRequest("123456", "123456", "pvt_admin_code")
        );

        assertEquals("new-admin-code-hash", organization.getAdminCodeHash());
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.ADMIN_CODE_CHANGE,
                "pvt_admin_code"
        );
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void updateAdminCode_rejectsConfirmationMismatch() {
        givenOrganization();

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> adminProfileService.updateAdminCode(
                        ORGANIZATION_ID,
                        new AdminCodeUpdateRequest("123456", "654321", "pvt_admin_code")
                )
        );

        assertEquals(ErrorCode.ADMIN_CODE_MISMATCH, exception.getErrorCode());
        verifyNoInteractions(passwordVerificationService);
    }

    private void givenOrganization() {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
    }
}
