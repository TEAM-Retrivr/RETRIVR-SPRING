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
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminCodeUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;

@ExtendWith(MockitoExtension.class)
class AdminProfileServiceTest {

    private static final Long ORGANIZATION_ID = 1L;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordVerificationService passwordVerificationService;

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
    void updateProfile_changesOnlyOrganizationName() {
        givenOrganization();

        var response = adminProfileService.updateProfile(
                ORGANIZATION_ID,
                new AdminProfileUpdateRequest(" New Org ")
        );

        assertEquals("New Org", response.organizationName());
        assertEquals("old@retrivr.com", response.email());
        assertEquals("old-password", organization.getPasswordHash());
        assertEquals("old-code", organization.getAdminCodeHash());
        verifyNoInteractions(passwordEncoder, passwordVerificationService);
    }

    @Test
    void updatePassword_changesPasswordAfterConsumingPurposeBoundToken() {
        givenOrganization();
        given(passwordEncoder.encode("NewPassword123!")).willReturn("new-password-hash");

        var response = adminProfileService.updatePassword(
                ORGANIZATION_ID,
                new AdminPasswordUpdateRequest(
                        "NewPassword123!",
                        "NewPassword123!",
                        "pvt_password"
                )
        );

        assertEquals(true, response.success());
        assertEquals("new-password-hash", organization.getPasswordHash());
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.PASSWORD_CHANGE,
                "pvt_password"
        );
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
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.PASSWORD_CHANGE,
                "pvt_password"
        );
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
    }

    @Test
    void updateAdminCode_changesCodeAfterConsumingPurposeBoundToken() {
        givenOrganization();
        given(passwordEncoder.encode("123456")).willReturn("new-admin-code-hash");

        var response = adminProfileService.updateAdminCode(
                ORGANIZATION_ID,
                new AdminCodeUpdateRequest("123456", "123456", "pvt_admin_code")
        );

        assertEquals(true, response.success());
        assertEquals("new-admin-code-hash", organization.getAdminCodeHash());
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.ADMIN_CODE_CHANGE,
                "pvt_admin_code"
        );
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
        verify(passwordVerificationService).validateAndConsume(
                ORGANIZATION_ID,
                PasswordVerificationPurpose.ADMIN_CODE_CHANGE,
                "pvt_admin_code"
        );
    }

    private void givenOrganization() {
        given(organizationRepository.findById(ORGANIZATION_ID))
                .willReturn(Optional.of(organization));
    }
}
