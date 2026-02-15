package retrivr.retrivrspring.presentation.item.res;

import java.util.List;
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

  public PublicItemSummary(Long itemId, Integer totalQuantity, Integer availableQuantity,
      String name, Integer duration, String description, String guaranteedGoods, Boolean isRentable,
      List<PublicItemUnitSummary> itemUnits) {
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
