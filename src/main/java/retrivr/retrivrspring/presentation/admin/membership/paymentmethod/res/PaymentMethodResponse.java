package retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentMethodStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;

public record PaymentMethodResponse(
    String paymentMethodId,
    PaymentProvider provider,
    PaymentMethodStatus status,
    boolean isDefault,
    LocalDateTime registeredAt,
    LocalDateTime disabledAt
) {

  public static PaymentMethodResponse from(PaymentMethod paymentMethod) {
    return new PaymentMethodResponse(
        paymentMethod.getId(),
        paymentMethod.getProvider(),
        paymentMethod.getStatus(),
        paymentMethod.isDefault(),
        paymentMethod.getRegisteredAt(),
        paymentMethod.getDisabledAt()
    );
  }
}
