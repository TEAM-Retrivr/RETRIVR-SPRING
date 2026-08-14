package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

public record BillingScheduleCancellationPreparation(
    String paymentId,
    String scheduleId,
    Long amount,
    boolean cancellationRequired,
    String subscriptionId,
    SubscriptionStatus subscriptionStatus,
    LocalDateTime canceledAt,
    LocalDateTime currentPassExpireAt
) {

  public static BillingScheduleCancellationPreparation notRequired(
      String paymentId,
      String subscriptionId,
      SubscriptionStatus subscriptionStatus,
      LocalDateTime canceledAt,
      LocalDateTime currentPassExpireAt
  ) {
    return new BillingScheduleCancellationPreparation(
        paymentId,
        null,
        null,
        false,
        subscriptionId,
        subscriptionStatus,
        canceledAt,
        currentPassExpireAt
    );
  }

  public static BillingScheduleCancellationPreparation cancellationRequired(
      Subscription subscription,
      Payment payment,
      LocalDateTime currentPassExpireAt
  ) {
    return new BillingScheduleCancellationPreparation(
        payment.getId(),
        payment.getPortOneScheduleId(),
        payment.getAmount(),
        true,
        subscription.getId(),
        subscription.getStatus(),
        subscription.getCanceledAt(),
        currentPassExpireAt
    );
  }
}
