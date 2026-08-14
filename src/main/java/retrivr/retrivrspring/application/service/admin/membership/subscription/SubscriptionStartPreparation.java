package retrivr.retrivrspring.application.service.admin.membership.subscription;

import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionStartPreparation(
    Long organizationId,
    String subscriptionId,
    String paymentMethodId,
    SubscriptionPlan plan
) {
}
