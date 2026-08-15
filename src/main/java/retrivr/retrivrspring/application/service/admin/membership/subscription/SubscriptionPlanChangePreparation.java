package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionPlanChangePreparation(
    String subscriptionId,
    SubscriptionPlan plan,
    LocalDateTime nextBillingAt,
    BillingScheduleCancellationPreparation cancellation
) {
}
