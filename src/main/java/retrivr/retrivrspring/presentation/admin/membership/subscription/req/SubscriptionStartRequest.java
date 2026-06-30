package retrivr.retrivrspring.presentation.admin.membership.subscription.req;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionStartRequest(
    @Schema(description = "구독 플랜", example = "MONTHLY")
    SubscriptionPlan plan
) {
}