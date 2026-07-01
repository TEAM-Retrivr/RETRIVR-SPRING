package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyPaymentResponse(
    PaymentSummary payment
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PaymentSummary(
      String pgTxId,
      OffsetDateTime paidAt
  ) {
  }
}
