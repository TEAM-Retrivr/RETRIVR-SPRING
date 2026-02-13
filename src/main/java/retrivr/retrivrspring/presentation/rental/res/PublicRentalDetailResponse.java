package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;

public record PublicRentalDetailResponse(
    Long rentalId,
    String itemName,
    String unitCode,
    String borrowerName,
    String borrowerMajor,
    String borrowerStudentNumber,
    LocalDate rentalDate,
    LocalDate returnDate
) {

}
