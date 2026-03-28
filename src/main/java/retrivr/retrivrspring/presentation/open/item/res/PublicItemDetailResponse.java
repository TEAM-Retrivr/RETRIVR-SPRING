package retrivr.retrivrspring.presentation.open.item.res;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

public record PublicItemDetailResponse(
    List<PublicItemUnitSummary> itemUnits,
    ItemManagementType itemManagementType
) {

  public record PublicItemUnitSummary(
      Long itemUnitId,
      String label,
      ItemUnitStatus status
  ) {

    public static PublicItemUnitSummary from(ItemUnit itemUnit) {
      return new PublicItemUnitSummary(
          itemUnit.getId(),
          itemUnit.getLabel(),
          itemUnit.getStatus()
      );
    }
  }
}
