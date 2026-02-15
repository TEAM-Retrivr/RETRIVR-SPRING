package retrivr.retrivrspring.presentation.rental.res;

import java.util.List;

public record AdminRentalItemPageResponse(
    List<RentalItemSummary> items,
    Long nextCursor
) {
  public record RentalItemSummary(
      Long itemId,
      String itemName,
      Integer totalQuantity,
      Integer availableQuantity,
      Boolean isRentalAvailable
  ) {

  }

}
