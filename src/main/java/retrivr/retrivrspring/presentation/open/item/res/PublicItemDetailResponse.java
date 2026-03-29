package retrivr.retrivrspring.presentation.open.item.res;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

public record PublicItemDetailResponse(
    List<PublicItemUnitSummary> itemUnits,
    List<BorrowerRequirement> borrowerRequirements,
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

  public record BorrowerRequirement(
      String label,
      boolean required
  ) {

    public static BorrowerRequirement from(ItemBorrowerField itemBorrowerField) {
      return new BorrowerRequirement(
          itemBorrowerField.getLabel(),
          itemBorrowerField.isRequired()
      );
    }
  }
}
