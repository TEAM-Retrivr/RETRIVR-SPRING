package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneScheduleBillingPaymentResponse(
    ScheduleSummary schedule
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ScheduleSummary(
      String id
  ) {
  }
}
