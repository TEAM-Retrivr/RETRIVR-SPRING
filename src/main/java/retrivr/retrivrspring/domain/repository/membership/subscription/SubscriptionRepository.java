package retrivr.retrivrspring.domain.repository.membership.subscription;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

  Optional<Subscription> findByOrganization(Organization organization);

  Optional<Subscription> findByPaymentMethod(PaymentMethod paymentMethod);

  Optional<Subscription> findByPaymentScheduleId(String paymentScheduleId);
}
