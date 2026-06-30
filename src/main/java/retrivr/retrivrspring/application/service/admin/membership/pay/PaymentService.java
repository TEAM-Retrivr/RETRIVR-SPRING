package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface PaymentService {

  Payment manualPayment(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      LocalDateTime now
  );

  Payment autoPayment(Subscription subscription, LocalDateTime now);

  Payment fail(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  );
}
