package retrivr.retrivrspring.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.auth.AdminCodeVerificationService;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import retrivr.retrivrspring.global.error.DomainException;

@ExtendWith(MockitoExtension.class)
class AdminProfileServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminCodeVerificationService adminCodeVerificationService;

    @InjectMocks
    private AdminProfileService adminProfileService;

    @Test
    @DisplayName("profile update success")
    void updateProfile_success() {
        Organization org = Organization.builder()
                .id(1L)
                .email("old@retrivr.com")
                .passwordHash("old-password")
                .name("old")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("old-code")
                .build();

        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
        given(organizationRepository.findByEmail("new@retrivr.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("NewPassword123!")).willReturn("encoded-password");
        given(passwordEncoder.encode("new-admin-code")).willReturn("encoded-admin-code");

        var response = adminProfileService.updateProfile(
                1L,
                new AdminProfileUpdateRequest(
                        "new@retrivr.com",
                        "NewPassword123!",
                        "NewPassword123!",
                        "New Org",
                        "new-admin-code",
                    "token"
                )
        );

        assertEquals("new@retrivr.com", response.email());
        assertEquals("New Org", response.organizationName());
        verify(passwordEncoder, times(1)).encode("NewPassword123!");
        verify(passwordEncoder, times(1)).encode("new-admin-code");
    }

    @Test
    @DisplayName("duplicate email throws ALREADY_EXIST_EXCEPTION")
    void updateProfile_duplicateEmail_throws() {
        Organization org = Organization.builder()
                .id(1L)
                .email("old@retrivr.com")
                .passwordHash("old-password")
                .name("old")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("old-code")
                .build();

        Organization anotherOrg = Organization.builder()
                .id(2L)
                .email("new@retrivr.com")
                .passwordHash("pw")
                .name("org")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("code")
                .build();

        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
        given(organizationRepository.findByEmail("new@retrivr.com")).willReturn(Optional.of(anotherOrg));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminProfileService.updateProfile(
                        1L,
                        new AdminProfileUpdateRequest(
                                "new@retrivr.com",
                                "NewPassword123!",
                                "NewPassword123!",
                                "New Org",
                                "new-admin-code",
                            "token"
                        )
                )
        );

        assertEquals(ErrorCode.ALREADY_EXIST_EXCEPTION, ex.getErrorCode());
        verify(passwordEncoder, times(1)).encode("NewPassword123!");
        verify(passwordEncoder, times(1)).encode("new-admin-code");
    }

    @Test
    @DisplayName("password confirm mismatch throws PASSWORD_RESET_PASSWORD_MISMATCH")
    void updateProfile_passwordMismatch_throws() {
        Organization org = Organization.builder()
                .id(1L)
                .email("old@retrivr.com")
                .passwordHash("old-password")
                .name("old")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("old-code")
                .build();

        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> adminProfileService.updateProfile(
                        1L,
                        new AdminProfileUpdateRequest(
                                "new@retrivr.com",
                                "NewPassword123!",
                                "DifferentPassword123!",
                                "New Org",
                                "new-admin-code",
                            "token"
                        )
                )
        );

        assertEquals(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH, ex.getErrorCode());
        verify(passwordEncoder, times(1)).encode("NewPassword123!");
        verify(passwordEncoder, times(1)).encode("new-admin-code");
    }

    @Test
    @DisplayName("disallowed special character in password throws INVALID_VALUE_EXCEPTION")
    void updateProfile_passwordWithDisallowedSpecialCharacter_throws() {
        Organization org = Organization.builder()
                .id(1L)
                .email("old@retrivr.com")
                .passwordHash("old-password")
                .name("old")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("old-code")
                .build();

        given(organizationRepository.findById(1L)).willReturn(Optional.of(org));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> adminProfileService.updateProfile(
                        1L,
                        new AdminProfileUpdateRequest(
                                "new@retrivr.com",
                                "NewPassword123?",
                                "NewPassword123?",
                                "New Org",
                                "new-admin-code",
                                "token"
                        )
                )
        );

        assertEquals(ErrorCode.INVALID_VALUE_EXCEPTION, ex.getErrorCode());
    }
}
