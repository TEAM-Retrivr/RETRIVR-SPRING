package retrivr.retrivrspring.presentation.admin.rental.res;

public record AdminMessageSendResponse(
    Long rentalId,
    boolean success
) {
}