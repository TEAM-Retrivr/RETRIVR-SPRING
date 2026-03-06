package retrivr.retrivrspring.presentation.admin.rental.res;


import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

public record AdminRentalReturnResponse(
    Long rentalId,
    RentalStatus rentalStatus,
    String adminNameToConfirm
) {

}
