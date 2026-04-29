package retrivr.retrivrspring.presentation.admin.rental.res;

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
      String itemUnitLabel,
      String borrowerName,
      String contact,
      Integer rentalDuration,
      LocalDate rentalDate,
      LocalDate dueDate,
      Integer overdueDays,
      LocalDate lastSentOverdueReminderDate,
      Boolean canSendOverdueSms
  ) {

    public static OverdueRentalItemSummary from(Rental rental, LocalDate lastSentOverdueReminderDate) {
      Item item = rental.getItem();
      ItemUnit itemUnit = rental.getItemUnit();
      Borrower borrower = rental.getBorrower();
      return new OverdueRentalItemSummary(
          rental.getId(),
          item.getId(),
          item.getName(),
          itemUnit != null ? itemUnit.getId() : null,
          itemUnit != null ? itemUnit.getLabel() : null,
          borrower.getName(),
          borrower.getPhoneNumber(),
          item.getRentalDuration(),
          rental.getDecidedAt().toLocalDate(),
          rental.getDueDate(),
          rental.getOverdueDays(),
          lastSentOverdueReminderDate,
          rental.canSendOverdueMessage()
      );
    }
  }

}
