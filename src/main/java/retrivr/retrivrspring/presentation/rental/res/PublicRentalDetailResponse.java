package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

public record PublicRentalDetailResponse(
    Long rentalId,
    RentalStatus rentalStatus,
    String itemName,
    String itemUnitCode,
    Map<String, String> borrowerField,
    LocalDateTime decidedAt,
    LocalDate dueDate
) {

  public static PublicRentalDetailResponse from(Rental rental, String itemName, String itemUnitCode, Map<String, String> borrowerField) {
    return new PublicRentalDetailResponse(
        rental.getId(),
        rental.getStatus(),
        itemName,
        itemUnitCode,
        borrowerField,
        rental.getDecidedAt(),
        rental.getDueDate()
    );
  }

}
