package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record AdminReturnItemUnitListPageResponse(
    Long itemId,
    String itemName,
    Integer availableQuantity,
    Integer totalQuantity,
    Integer rentalDuration,
    List<ReturnItemUnitSummary> itemUnits,
    Long nextCursor
) {

  public record ReturnItemUnitSummary(
      boolean isOverdue,
      Long unitId,
      String unitCode,
      String borrowerName,
      String borrowerStudentNumber,
      String borrowerMajor,
      LocalDate rentalDate,
      LocalDate expectedReturnDueDate
  ) {

    public static ReturnItemUnitSummary from(ItemUnit itemUnit, Rental rental) {
      Borrower borrower = rental.getBorrower();
      return new ReturnItemUnitSummary(
          rental.isOverdue(),
          itemUnit.getId(),
          itemUnit.getCode(),
          borrower.getName(),
          borrower.getStudentNumber(),
          borrower.getMajor(),
          rental.getRequestedAt().toLocalDate(),
          rental.getDueDate()
      );
    }

  }

}
