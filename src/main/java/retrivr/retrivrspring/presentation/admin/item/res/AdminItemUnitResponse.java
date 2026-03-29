package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

@Schema(description = "관리자 물품 유닛 응답")
public record AdminItemUnitResponse(
    @Schema(description = "유닛 ID", example = "101")
    Long itemUnitId,

    @Schema(description = "유닛 라벨", example = "기본 충전기")
    String label,

    @Schema(description = "유닛 상태", example = "AVAILABLE")
    ItemUnitStatus status
) {

  public static AdminItemUnitResponse from(ItemUnit itemUnit) {
    return new AdminItemUnitResponse(
        itemUnit.getId(),
        itemUnit.getLabel(),
        itemUnit.getStatus()
    );
  }
}
