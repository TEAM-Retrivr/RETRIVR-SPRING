package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneCancelScheduledPaymentRequest(
    String storeId,
    String billingKey,
    List<String> scheduleIds
) {

  public static PortOneCancelScheduledPaymentRequest byScheduleId(String scheduleId) {
    return new PortOneCancelScheduledPaymentRequest(null, null, List.of(scheduleId));
  }

  public PortOneCancelScheduledPaymentRequest withDefaultStoreId(String defaultStoreId) {
    return new PortOneCancelScheduledPaymentRequest(
        storeId != null ? storeId : defaultStoreId,
        billingKey,
        scheduleIds
    );
  }

  @Override
  public String toString() {
    return "PortOneCancelScheduledPaymentRequest[scheduleIds=" + scheduleIds + "]";
  }
}
