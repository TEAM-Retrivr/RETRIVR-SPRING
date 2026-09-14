package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;

public record AdminItemUnitDeletionResult(
    Long itemUnitId,
    String label,
    ItemUnitDeletionType deletionType
) {
  public static AdminItemUnitDeletionResult hardDelete(ItemUnit itemUnit) {
    return new AdminItemUnitDeletionResult(
        itemUnit.getId(), itemUnit.getLabel(), ItemUnitDeletionType.HARD_DELETE);
  }

  public static AdminItemUnitDeletionResult softDelete(ItemUnit itemUnit) {
    return new AdminItemUnitDeletionResult(
        itemUnit.getId(), itemUnit.getLabel(), ItemUnitDeletionType.SOFT_DELETE);
  }

  @Schema(description = "고유번호 삭제 방식")
  public enum ItemUnitDeletionType {
    HARD_DELETE,
    SOFT_DELETE
  }
}
