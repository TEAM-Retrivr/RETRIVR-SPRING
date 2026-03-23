package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

@Schema(description = "UNIT 물품 유닛 상태 변경 응답")
public record AdminItemUnitMutationResponse(
    @Schema(description = "물품 ID", example = "12")
    Long itemId,
    @Schema(description = "유닛 ID", example = "101")
    Long itemUnitId,
    @Schema(description = "유닛 라벨", example = "기본 충전기")
    String label,
    @Schema(description = "변경 후 유닛 상태", example = "INACTIVE")
    ItemUnitStatus status,
    @Schema(description = "변경 후 총 수량", example = "5")
    Integer totalQuantity,
    @Schema(description = "변경 후 대여 가능 수량", example = "3")
    Integer availableQuantity
) {

  public static AdminItemUnitMutationResponse from(Item item, ItemUnit itemUnit) {
    return new AdminItemUnitMutationResponse(
        item.getId(),
        itemUnit.getId(),
        itemUnit.getLabel(),
        itemUnit.getStatus(),
        item.getTotalQuantity(),
        item.getAvailableQuantity()
    );
  }
}
