package retrivr.retrivrspring.application.service.admin.membership.pay.schedule;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCustomerRequest;

public record BillingSchedulePreparation(
    String paymentId,
    String subscriptionId,
    String billingKey,
    PaymentProvider provider,
    SubscriptionPlan plan,
    long amount,
    LocalDateTime billingAt,
    PortOneCustomerRequest customer,
    boolean alreadyScheduled
) {
}
