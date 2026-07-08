package retrivr.retrivrspring.application.service.admin.account;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.WithdrawalReasonCode;
import retrivr.retrivrspring.domain.repository.auth.RefreshTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.account.req.AdminWithdrawRequest;
import retrivr.retrivrspring.presentation.admin.account.res.AdminWithdrawResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWithdrawalService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawalFeedbackService withdrawalFeedbackService;

    @Transactional
    public AdminWithdrawResponse withdraw(AuthUser authUser, AdminWithdrawRequest request) {
        Organization organization = organizationRepository.findById(authUser.organizationId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), organization.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PASSWORD_MISMATCH);
        }

        validateReasons(request.reasonCodes(), request.otherReason());

        try {
            withdrawalFeedbackService.save(organization, request.reasonCodes(), normalizeOtherReason(request.otherReason()));
        } catch (Exception e) {
            log.warn("Failed to save withdrawal feedback for organizationId={}", organization.getId(), e);
        }

        organization.withdraw(LocalDateTime.now());
        refreshTokenRepository.deleteAllByEmail(organization.getEmail());
        SecurityContextHolder.clearContext();

        return AdminWithdrawResponse.ok();
    }

    private void validateReasons(List<WithdrawalReasonCode> reasonCodes, String otherReason) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            throw new ApplicationException(ErrorCode.WITHDRAW_REASON_REQUIRED);
        }

        boolean hasOther = reasonCodes.contains(WithdrawalReasonCode.OTHER);
        String normalizedOtherReason = normalizeOtherReason(otherReason);
        if (hasOther && normalizedOtherReason == null) {
            throw new ApplicationException(ErrorCode.WITHDRAW_OTHER_REASON_REQUIRED);
        }
    }

    private String normalizeOtherReason(String otherReason) {
        if (otherReason == null) {
            return null;
        }
        String trimmed = otherReason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
