package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface PaymentService {

  Payment charge(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      LocalDateTime now
  );

  Payment fail(
      String paymentId,
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  );
}
