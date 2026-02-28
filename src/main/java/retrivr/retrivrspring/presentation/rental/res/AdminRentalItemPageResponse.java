package retrivr.retrivrspring.presentation.rental.res;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;

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

    public static RentalItemSummary from(Item item) {
      return new RentalItemSummary(
          item.getId(),
          item.getName(),
          item.getTotalQuantity(),
          item.getAvailableQuantity(),
          item.isRentalAble()
      );
    }
  }

}
