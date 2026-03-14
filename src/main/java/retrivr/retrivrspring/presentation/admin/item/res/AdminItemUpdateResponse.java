package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

@Schema(description = "관리자 물품 수정 응답")
public record AdminItemUpdateResponse(
    @Schema(description = "물품 ID", example = "12")
    Long itemId,

    @Schema(description = "물품명", example = "C타입 고속 충전기")
    String name,

    @Schema(description = "물품 설명", example = "고속 충전 지원 충전기")
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "5")
    Integer rentalDuration,

    @Schema(description = "활성 여부", example = "true")
    Boolean isActive,

    @Schema(description = "물품 관리 방식", example = "UNIT")
    ItemManagementType itemManagementType,

    @Schema(description = "대여자 입력 요구 정보 목록")
    List<BorrowerRequirementResponse> borrowerRequirements
) {

  public static AdminItemUpdateResponse from(Item item, List<ItemBorrowerField> borrowerFields) {
    return new AdminItemUpdateResponse(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getRentalDuration(),
        item.isActive(),
        item.getItemManagementType(),
        borrowerFields.stream()
            .map(BorrowerRequirementResponse::from)
            .toList()
    );
  }
}
