package retrivr.retrivrspring.application.service.admin.membership.pay.immediate;

import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCustomerRequest;

public record ImmediatePaymentPreparation(
    String paymentId,
    String billingKey,
    PaymentProvider provider,
    SubscriptionPlan plan,
    long amount,
    PortOneCustomerRequest customer
) {
}
