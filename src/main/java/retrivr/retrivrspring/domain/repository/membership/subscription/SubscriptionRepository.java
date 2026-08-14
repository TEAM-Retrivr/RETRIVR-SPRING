package retrivr.retrivrspring.domain.repository.membership.subscription;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

  Optional<Subscription> findByOrganization(Organization organization);

  Optional<Subscription> findByPaymentMethod(PaymentMethod paymentMethod);

  Optional<Subscription> findByPaymentScheduleId(String paymentScheduleId);

  @Query("""
      select s.id
      from Subscription s
      where s.status = :subscriptionStatus
        and s.nextBillingAt is not null
        and not exists (
          select p.id
          from Payment p
          where p.organization = s.organization
            and p.status in :paymentStatuses
        )
      order by s.nextBillingAt asc
      """)
  List<String> findIdsMissingBillingSchedule(
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus,
      @Param("paymentStatuses") List<PaymentStatus> paymentStatuses,
      Pageable pageable
  );
}
