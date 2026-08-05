package retrivr.retrivrspring.presentation.admin.membership.subscription.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

public record SubscriptionCancelResponse(
    String subscriptionId,
    SubscriptionStatus status,
    LocalDateTime canceledAt,
    LocalDateTime currentPassExpireAt
) {

}
