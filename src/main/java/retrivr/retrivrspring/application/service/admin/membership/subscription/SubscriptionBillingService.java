package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.BillingResult;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBillingService {

  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public BillingResult billIfAvailable(Organization organization, LocalDateTime now) {
    Subscription subscription = subscriptionRepository
        .findByOrganization(organization)
        .orElse(null);

    if (subscription == null || !subscription.isActive()) {
      return BillingResult.NOT_SUBSCRIBED;
    }
/*
    if (payment.isSuccess()) {
      subscription.completeSuccessfulPayment();
      MembershipPass membershipPass = membershipPassService.generateSubscriptionMembershipPass(
          organization.getId(),
          subscription
      );
      subscription.scheduleNextBillingAt(membershipPass.getEndAt());
      return BillingResult.PAYMENT_SUCCEEDED;
    }

 */

    subscription.failPayment(now);

    if (subscription.isPastDue()) {
      return BillingResult.PAYMENT_RETRYABLE_FAILED;
    }

    return BillingResult.PAYMENT_FINAL_FAILED;
  }
}
