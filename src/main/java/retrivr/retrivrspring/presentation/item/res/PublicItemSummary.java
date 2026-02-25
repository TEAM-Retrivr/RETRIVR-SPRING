package retrivr.retrivrspring.presentation.item.res;

import retrivr.retrivrspring.domain.entity.item.Item;

public record PublicItemSummary(
    Long itemId,
    String name,
    Integer availableQuantity,
    Integer totalQuantity,
    Boolean isActive,
    Integer rentalDuration,
    String description,
    String guaranteedGoods
) {

  public static PublicItemSummary from(Item item) {
    return new PublicItemSummary(
        item.getId(),
        item.getName(),
        item.getAvailableQuantity(),
        item.getTotalQuantity(),
        item.isActive(),
        item.getRentalDuration(),
        item.getDescription(),
        item.getGuaranteedGoods()
    );

  }
}
