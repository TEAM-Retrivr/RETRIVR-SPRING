package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancelPaymentResponse(
    Cancellation cancellation
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Cancellation(
      String id,
      String status
  ) {
  }
}
