package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
    String status,
    String id,
    String transactionId,
    String storeId,
    PortOneSelectedChannelResponse channel,
    String orderName,
    PortOnePaymentAmountResponse amount,
    String currency,
    String billingKey,
    String scheduleId,
    OffsetDateTime paidAt,
    OffsetDateTime failedAt,
    PortOnePaymentFailureResponse failure
) {

  public boolean hasPaymentId(String paymentId) {
    return id != null && id.equals(paymentId);
  }

  public boolean hasTotalAmount(long expectedAmount) {
    return amount != null && amount.total() != null && amount.total() == expectedAmount;
  }

  public boolean isPaid() {
    return "PAID".equals(status);
  }

  public boolean isFailed() {
    return "FAILED".equals(status);
  }

  public boolean isCancelled() {
    return "CANCELLED".equals(status);
  }
}
