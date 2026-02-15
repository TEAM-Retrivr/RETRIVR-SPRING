package retrivr.retrivrspring.presentation.item.res;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class PublicItemSummary {
  Long itemId;
  Integer totalQuantity;
  Integer availableQuantity;
  String name;
  Integer duration;
  String description;
  String guaranteedGoods;
  Boolean isRentable;
  Boolean hasItemUnit;
  List<PublicItemUnitSummary> itemUnits;

  public PublicItemSummary(Long itemId, int totalQuantity, Integer availableQuantity,
      String name, Integer duration, String description, String guaranteedGoods, boolean isRentable,
      List<PublicItemUnitSummary> itemUnits) {
    if (itemUnits == null) itemUnits = List.of();
    this.itemId = itemId;
    this.totalQuantity = totalQuantity;
    this.availableQuantity = availableQuantity;
    this.name = name;
    this.duration = duration;
    this.description = description;
    this.guaranteedGoods = guaranteedGoods;
    this.isRentable = totalQuantity > 0 && isRentable;
    this.hasItemUnit = !itemUnits.isEmpty();
    this.itemUnits = itemUnits;
  }

  public record PublicItemUnitSummary(
      Long itemUnitId,
      String itemUnitCode,
      Boolean isRentable
  ) {

  }
}
