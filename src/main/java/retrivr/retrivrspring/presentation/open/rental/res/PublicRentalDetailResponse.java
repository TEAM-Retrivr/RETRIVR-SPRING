package retrivr.retrivrspring.presentation.open.rental.res;

import java.time.LocalDateTime;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record PublicRentalDetailResponse(
    Long rentalId,
    String itemName,
    Integer rentalDuration,
    String itemUnitLabel,
    String contact,
    String guaranteedGoods,
    Map<String, String> borrowerField,
    String requestNote,
    LocalDateTime requestedAt
) {

  public static PublicRentalDetailResponse from(
      Rental rental,
      String itemName,
      Integer rentalDuration,
      String itemUnitLabel,
      String contact,
      String guaranteedGoods,
      Map<String, String> borrowerField
  ) {
    return new PublicRentalDetailResponse(
        rental.getId(),
        itemName,
        rentalDuration,
        itemUnitLabel,
        contact,
        guaranteedGoods,
        borrowerField,
        rental.getRequestNote(),
        rental.getRequestedAt()
    );
  }

}
