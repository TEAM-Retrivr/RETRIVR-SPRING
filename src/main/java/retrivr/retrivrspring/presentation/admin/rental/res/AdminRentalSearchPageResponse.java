package retrivr.retrivrspring.presentation.admin.rental.res;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record AdminRentalSearchPageResponse(
  List<RentalSearchSummary> rentals,
  Double nextScoreCursor,
  Long nextRentalIdCursor
) {

  public record RentalSearchSummary(
      Long rentalId,
      String borrowerName,
      String contact,
      String itemName,
      boolean isOverdue,
      String itemUnitLabel,
      LocalDate rentalDate,
      LocalDate expectedReturnDueDate,
      Map<String, String> borrowerFields,
      String requestNote,
      String approvalAdminName
  ) {
    public static RentalSearchSummary from(Rental rental) {
      return new RentalSearchSummary(
          rental.getId(),
          rental.getBorrower().getName(),
          rental.getBorrower().getContact(),
          rental.getItem().getName(),
          rental.isOverdue(),
          rental.getItemUnit() != null ? rental.getItemUnit().getLabel() : null,
          rental.getDecidedAt().toLocalDate(),
          rental.getDueDate(),
          BorrowerFieldsResponseMapper.from(rental.getBorrower()),
          rental.getRequestNote(),
          rental.getDecidedBy()
      );
    }
  }
}
