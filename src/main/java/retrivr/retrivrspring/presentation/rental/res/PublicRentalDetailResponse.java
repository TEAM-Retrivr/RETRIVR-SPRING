package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import retrivr.retrivrspring.entity.rental.enumerate.RentalStatus;

public record PublicRentalDetailResponse(
    Long rentalId,
    RentalStatus rentalStatus,
    String itemName,
    String itemUnitCode,
    String borrowerName,
    String borrowerMajor,
    String borrowerStudentNumber,
    LocalDate rentalDate,
    LocalDate returnDueDate
) {

}
