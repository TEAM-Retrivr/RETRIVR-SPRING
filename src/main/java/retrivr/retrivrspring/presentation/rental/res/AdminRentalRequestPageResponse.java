package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRentalRequestPageResponse(
    List<RentalRequestSummary> requests,
    Long nextCursor
) {
  public record RentalRequestSummary(
      Long rentalId,
      Long itemId,
      String itemName,
      Long itemUnitId,
      String itemUnitCode,
      Integer totalQuantity,
      Integer availableQuantity,
      String borrowerName,
      String borrowerMajor,
      String borrowerStudentNumber,
      String guaranteedGoods,
      LocalDateTime requestedAt
  ){

  }

}
