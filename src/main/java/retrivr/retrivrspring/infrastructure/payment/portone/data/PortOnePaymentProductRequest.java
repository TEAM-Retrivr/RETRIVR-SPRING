package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOnePaymentProductRequest(
    String id,
    String name,
    Long amount,
    Integer quantity,
    String tag,
    String code
) {
}
