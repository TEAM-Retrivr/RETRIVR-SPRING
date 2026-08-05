package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneScheduleBillingPaymentBody(
    PortOneBillingKeyPaymentBody payment,
    OffsetDateTime timeToPay
) {
}
