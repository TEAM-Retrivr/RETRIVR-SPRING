package retrivr.retrivrspring.presentation.admin.membership.subscription.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionPlanChangeRequest(
    @Schema(description = "변경할 구독 플랜", example = "YEARLY")
    @NotNull
    SubscriptionPlan plan
) {
}
