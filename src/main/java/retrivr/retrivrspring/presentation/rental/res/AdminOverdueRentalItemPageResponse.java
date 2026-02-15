package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import java.util.List;

public record AdminOverdueRentalItemPageResponse(
  List<OverdueRentalItemSummary> rentals,
  Long nextCursor
) {
  public record OverdueRentalItemSummary(
    Long rentalId,
    Long itemId,
    String itemName,
    Long itemUnitId,
    String itemUnitCode,
    String borrowerName,
    String borrowerStudentNumber,
    String borrowerMajor,
    LocalDate rentalDate,
    LocalDate dueDate,
    Integer overdueDays,
    List<LocalDate> sendOverdueSmsDates,
    Boolean canSendOverdueSms
  ){

  }

}
