package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import java.util.List;

public record AdminReturnItemUnitListPageResponse(
    Boolean isRentalAvailable,
    Long itemId,
    String itemName,
    Integer totalQuantity,
    Integer rentalDuration,
    List<ReturnItemUnitSummary> itemUnits,
    Long nextCursor
) {

  public record ReturnItemUnitSummary(
      Long unitId,
      String unitCode,
      String borrowerName,
      String borrowerStudentNumber,
      String borrowerMajor,
      LocalDate rentalDate,
      LocalDate expectedReturnDueDate
  ) {

  }

}
