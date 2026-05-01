package retrivr.retrivrspring.presentation.open.rental.res;

import java.time.LocalDateTime;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record PublicRentalDetailResponse(
    Long rentalId,
    String itemName,
    String itemUnitLabel,
    Map<String, String> borrowerField,
    String requestNote,
    LocalDateTime requestedAt
) {

  public static PublicRentalDetailResponse from(
      Rental rental,
      String itemName,
      String itemUnitLabel,
      Map<String, String> borrowerField
  ) {
    return new PublicRentalDetailResponse(
        rental.getId(),
        itemName,
        itemUnitLabel,
        borrowerField,
        rental.getRequestNote(),
        rental.getRequestedAt()
    );
  }

}
