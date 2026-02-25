package retrivr.retrivrspring.presentation.item.res;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

public record PublicItemDetailResponse(
    List<PublicItemUnitSummary> itemUnits,
    ItemManagementType itemManagementType
) {

  public record PublicItemUnitSummary(
      Long itemUnitId,
      String code,
      ItemUnitStatus status
  ) {

    public static PublicItemUnitSummary from(ItemUnit itemUnit) {
      return new PublicItemUnitSummary(
          itemUnit.getId(),
          itemUnit.getCode(),
          itemUnit.getStatus()
      );
    }
  }
}
