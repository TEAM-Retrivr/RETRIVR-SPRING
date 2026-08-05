package retrivr.retrivrspring.presentation.admin.membership.subscription.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionStartRequest(
    @Schema(description = "구독 플랜", example = "MONTHLY")
    SubscriptionPlan plan,

    @Schema(description = "결제 수단 ID", example = "payment-method-id")
    @NotBlank
    String paymentMethodId
) {
}