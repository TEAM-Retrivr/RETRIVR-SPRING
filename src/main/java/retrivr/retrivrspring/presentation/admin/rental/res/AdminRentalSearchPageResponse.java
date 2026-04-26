package retrivr.retrivrspring.presentation.admin.rental.res;

import java.util.List;
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
      String itemName
  ) {
    public static RentalSearchSummary from(Rental rental) {
      return new RentalSearchSummary(
          rental.getId(),
          rental.getBorrower().getName(),
          rental.getBorrower().getPhone() != null ? rental.getBorrower().getPhone().getPhone() : null,
          rental.getItem().getName()
      );
    }
  }
}
