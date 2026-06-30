package retrivr.retrivrspring.presentation.admin.membership.subscription.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

public record SubscriptionStartResponse(
    String subscriptionId,
    SubscriptionPlan plan,
    SubscriptionStatus status,
    LocalDateTime nextBillingAt,
    String membershipPassId,
    LocalDateTime startAt,
    LocalDateTime expireAt
) {

}