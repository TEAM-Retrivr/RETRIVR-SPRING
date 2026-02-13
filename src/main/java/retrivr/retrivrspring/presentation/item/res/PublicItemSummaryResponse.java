package retrivr.retrivrspring.presentation.item.res;

import java.util.List;
import lombok.Getter;

@Getter
public class PublicItemSummaryResponse {

  Long itemId;
  Integer totalQuantity;
  Integer availableQuantity;
  String name;
  Integer duration;
  String description;
  String guaranteedGoods;
  Boolean isRentable;
  Boolean hasItemUnit;
  List<PublicItemUnitResponse> itemUnits;

  public PublicItemSummaryResponse(Long itemId, Integer totalQuantity, Integer availableQuantity,
      String name, Integer duration, String description, String guaranteedGoods, Boolean isRentable,
      List<PublicItemUnitResponse> itemUnits) {
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

  public record PublicItemUnitResponse(
      Long unitId,
      String unitCode,
      Boolean isRentable
  ) {

  }
}
