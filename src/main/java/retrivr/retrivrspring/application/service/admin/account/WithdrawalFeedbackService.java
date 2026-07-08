package retrivr.retrivrspring.application.service.admin.account;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.WithdrawalFeedback;
import retrivr.retrivrspring.domain.entity.organization.WithdrawalFeedbackReason;
import retrivr.retrivrspring.domain.entity.organization.enumerate.WithdrawalReasonCode;
import retrivr.retrivrspring.domain.repository.organization.WithdrawalFeedbackRepository;

@Service
@RequiredArgsConstructor
public class WithdrawalFeedbackService {

    private final WithdrawalFeedbackRepository withdrawalFeedbackRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Organization organization, List<WithdrawalReasonCode> reasonCodes, String otherReason) {
        WithdrawalFeedback feedback = WithdrawalFeedback.builder()
                .organization(organization)
                .otherReason(otherReason)
                .build();

        for (WithdrawalReasonCode reasonCode : reasonCodes) {
            feedback.addReason(WithdrawalFeedbackReason.builder()
                    .feedback(feedback)
                    .reasonCode(reasonCode)
                    .build());
        }

        withdrawalFeedbackRepository.save(feedback);
    }
}
