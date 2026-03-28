package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

@Schema(description = "관리자 물품 상세 응답")
public record AdminItemDetailResponse(
    @Schema(description = "물품 ID", example = "12")
    Long itemId,

    @Schema(description = "물품명", example = "C타입 충전기")
    String name,

    @Schema(description = "물품 설명", example = "고속 충전 지원 충전기")
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "3")
    Integer rentalDuration,

    @Schema(description = "물품 총 개수(개)", example = "3")
    Integer totalQuantity,

    @Schema(description = "활성 여부", example = "true")
    Boolean isActive,

    @Schema(description = "물품 관리 방식", example = "NON_UNIT")
    ItemManagementType itemManagementType,

    @Schema(description = "카카오톡 문자 발송 여부", example = "true")
    Boolean useMessageAlarmService,

    @Schema(description = "담보물품 여부 및 종류", example = "학생증")
    String guaranteedGoods,

    @Schema(description = "유닛 목록")
    List<AdminItemUnitResponse> itemUnits,

    @Schema(description = "대여자 입력 요구 정보 목록")
    List<BorrowerRequirementResponse> borrowerRequirements
) {

  public static AdminItemDetailResponse from(
      Item item,
      List<ItemBorrowerField> borrowerFields,
      List<ItemUnit> itemUnits
  ) {
    return new AdminItemDetailResponse(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getRentalDuration(),
        item.getTotalQuantity(),
        item.isActive(),
        item.getItemManagementType(),
        item.isUseMessageAlarmService(),
        item.getGuaranteedGoods(),
        itemUnits.stream()
            .map(AdminItemUnitResponse::from)
            .toList(),
        borrowerFields.stream()
            .map(BorrowerRequirementResponse::from)
            .toList()
    );
  }
}
