package retrivr.retrivrspring.presentation.admin.membership.subscription.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionPlanChangeResponse(
    String subscriptionId,
    SubscriptionPlan plan,
    LocalDateTime nextBillingAt
) {
}
