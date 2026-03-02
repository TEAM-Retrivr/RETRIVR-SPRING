package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;

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
  ) {

    public static OverdueRentalItemSummary from(Rental rental) {
      Item item = rental.getItem();
      ItemUnit itemUnit = rental.getItemUnit();
      Borrower borrower = rental.getBorrower();
      return new OverdueRentalItemSummary(
          rental.getId(),
          item.getId(),
          item.getName(),
          itemUnit != null ? itemUnit.getId() : null,
          itemUnit != null ? itemUnit.getCode() : null,
          borrower.getName(),
          borrower.getStudentNumber(),
          borrower.getMajor(),
          rental.getDecidedAt().toLocalDate(),
          rental.getDueDate(),
          rental.getOverdueDays(),
          List.of(),
          rental.canSendOverdueSms()
      );
    }
  }

}
