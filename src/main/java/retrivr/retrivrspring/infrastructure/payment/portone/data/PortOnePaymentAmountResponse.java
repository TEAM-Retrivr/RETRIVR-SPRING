package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentAmountResponse(
    Long total,
    Long taxFree,
    Long vat,
    Long supply,
    Long discount,
    Long paid,
    Long cancelled,
    Long cancelledTaxFree
) {
}
