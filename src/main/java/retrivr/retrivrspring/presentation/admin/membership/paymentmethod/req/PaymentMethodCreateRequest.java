package retrivr.retrivrspring.presentation.admin.membership.paymentmethod.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;

public record PaymentMethodCreateRequest(
    @Schema(description = "결제 PG", example = "KAKAOPAY")
    @NotNull
    PaymentProvider provider,

    @Schema(description = "PortOne V2 billingKey", example = "billing-key")
    @NotBlank
    String billingKey,

    @Schema(description = "기본 결제수단 지정 여부", example = "true")
    boolean isDefault
) {
}
