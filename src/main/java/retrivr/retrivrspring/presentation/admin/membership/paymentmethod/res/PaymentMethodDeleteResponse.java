package retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentMethodStatus;

public record PaymentMethodDeleteResponse(
    String paymentMethodId,
    PaymentMethodStatus status,
    LocalDateTime disabledAt
) {
}
