package retrivr.retrivrspring.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.account.AdminWithdrawalService;
import retrivr.retrivrspring.application.service.admin.account.WithdrawalFeedbackService;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.organization.enumerate.WithdrawalReasonCode;
import retrivr.retrivrspring.domain.repository.auth.RefreshTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.account.req.AdminWithdrawRequest;

@ExtendWith(MockitoExtension.class)
class AdminWithdrawalServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private WithdrawalFeedbackService withdrawalFeedbackService;

    @InjectMocks
    private AdminWithdrawalService adminWithdrawalService;

    @Test
    void withdraw_success() {
        Organization organization = activeOrganization();
        AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");
        AdminWithdrawRequest request = new AdminWithdrawRequest(
                "Password123!",
                List.of(WithdrawalReasonCode.LOW_USAGE),
                null,
                true
        );

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(passwordEncoder.matches("Password123!", organization.getPasswordHash())).willReturn(true);

        var response = adminWithdrawalService.withdraw(authUser, request);

        assertTrue(response.success());
        assertEquals(OrganizationStatus.WITHDRAWN, organization.getStatus());
        assertNotNull(organization.getWithdrawnAt());
        verify(withdrawalFeedbackService).save(eq(organization), eq(request.reasonCodes()), eq(null));
        verify(refreshTokenRepository).deleteAllByEmail("admin@retrivr.com");
    }

    @Test
    void withdraw_feedbackSaveFails_stillWithdraws() {
        Organization organization = activeOrganization();
        AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");
        AdminWithdrawRequest request = new AdminWithdrawRequest(
                "Password123!",
                List.of(WithdrawalReasonCode.OTHER),
                "기타 사유",
                true
        );

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(passwordEncoder.matches("Password123!", organization.getPasswordHash())).willReturn(true);
        doThrow(new RuntimeException("save failed"))
                .when(withdrawalFeedbackService)
                .save(any(), any(), any());

        var response = adminWithdrawalService.withdraw(authUser, request);

        assertTrue(response.success());
        assertEquals(OrganizationStatus.WITHDRAWN, organization.getStatus());
        verify(refreshTokenRepository).deleteAllByEmail("admin@retrivr.com");
    }

    @Test
    void withdraw_passwordMismatch_throws() {
        Organization organization = activeOrganization();
        AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");
        AdminWithdrawRequest request = new AdminWithdrawRequest(
                "wrong-password",
                List.of(WithdrawalReasonCode.LOW_USAGE),
                null,
                true
        );

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(passwordEncoder.matches("wrong-password", organization.getPasswordHash())).willReturn(false);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> adminWithdrawalService.withdraw(authUser, request)
        );

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(refreshTokenRepository, never()).deleteAllByEmail(any());
    }

    @Test
    void withdraw_withoutReasons_throws() {
        Organization organization = activeOrganization();
        AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");
        AdminWithdrawRequest request = new AdminWithdrawRequest(
                "Password123!",
                List.of(),
                null,
                true
        );

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(passwordEncoder.matches("Password123!", organization.getPasswordHash())).willReturn(true);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> adminWithdrawalService.withdraw(authUser, request)
        );

        assertEquals(ErrorCode.WITHDRAW_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    void withdraw_otherReasonMissing_throws() {
        Organization organization = activeOrganization();
        AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");
        AdminWithdrawRequest request = new AdminWithdrawRequest(
                "Password123!",
                List.of(WithdrawalReasonCode.OTHER),
                "   ",
                true
        );

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(passwordEncoder.matches("Password123!", organization.getPasswordHash())).willReturn(true);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> adminWithdrawalService.withdraw(authUser, request)
        );

        assertEquals(ErrorCode.WITHDRAW_OTHER_REASON_REQUIRED, exception.getErrorCode());
    }

    private Organization activeOrganization() {
        return Organization.builder()
                .id(1L)
                .email("admin@retrivr.com")
                .passwordHash("$2a$10$mockhashedpasswordhashhashhash")
                .status(OrganizationStatus.ACTIVE)
                .adminCodeHash("encoded-admin-code")
                .build();
    }
}
