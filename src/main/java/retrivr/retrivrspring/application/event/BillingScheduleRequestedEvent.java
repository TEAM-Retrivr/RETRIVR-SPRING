package retrivr.retrivrspring.application.event;

import java.time.LocalDateTime;

public record BillingScheduleRequestedEvent(
    String subscriptionId,
    LocalDateTime billingAt
) {
}
