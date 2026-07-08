package retrivr.retrivrspring.domain.repository.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.WithdrawalFeedback;

public interface WithdrawalFeedbackRepository extends JpaRepository<WithdrawalFeedback, Long> {
}
