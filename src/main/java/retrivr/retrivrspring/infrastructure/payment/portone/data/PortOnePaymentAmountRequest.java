package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOnePaymentAmountRequest(
    Long total,
    Long taxFree,
    Long vat
) {

  public static PortOnePaymentAmountRequest total(long total) {
    return new PortOnePaymentAmountRequest(total, null, null);
  }
}
