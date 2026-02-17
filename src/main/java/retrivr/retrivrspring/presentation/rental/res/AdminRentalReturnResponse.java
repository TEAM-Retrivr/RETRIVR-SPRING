package retrivr.retrivrspring.presentation.rental.res;


import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

public record AdminRentalReturnResponse(
    Long rentalId,
    RentalStatus rentalStatus,
    String adminNameToConfirm
) {

}
