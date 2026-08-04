package retrivr.retrivrspring.application.event;

public record ScheduledPaymentFailEvent(
    String paymentId
) {

}
