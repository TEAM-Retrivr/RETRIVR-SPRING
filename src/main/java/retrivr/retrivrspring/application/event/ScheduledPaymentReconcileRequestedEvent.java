package retrivr.retrivrspring.application.event;

public record ScheduledPaymentReconcileRequestedEvent(
    String paymentId
) {
}
