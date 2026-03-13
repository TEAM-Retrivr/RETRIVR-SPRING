package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

@Schema(description = "관리자 물품 등록 응답")
public record AdminItemCreateResponse(
    @Schema(description = "물품 ID", example = "12")
    Long itemId,

    @Schema(description = "물품명", example = "C타입 충전기")
    String name,

    @Schema(description = "물품 설명", example = "고속 충전 지원 충전기")
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "3")
    Integer rentalDuration,

    @Schema(description = "활성 여부", example = "true")
    Boolean isActive,

    @Schema(description = "물품 관리 방식", example = "NON_UNIT")
    ItemManagementType itemManagementType,

    @Schema(description = "대여자 입력 요구 정보 목록")
    List<AdminItemBorrowerRequirementResponse> borrowerRequirements
) {

  public static AdminItemCreateResponse from(Item item, List<ItemBorrowerField> borrowerFields) {
    return new AdminItemCreateResponse(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getRentalDuration(),
        item.isActive(),
        item.getItemManagementType(),
        borrowerFields.stream()
            .map(AdminItemBorrowerRequirementResponse::from)
            .toList()
    );
  }
}
