package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

@Schema(description = "관리자 물품 목록 항목")
public record AdminItemListResponse(

    @Schema(description = "물품 ID", example = "11")
    Long itemId,

    @Schema(description = "물품명", example = "노트북")
    String name,

    @Schema(description = "물품 설명", example = "대여용 노트북")
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "7")
    Integer rentalDuration,

    @Schema(description = "총 수량", example = "10")
    int totalQuantity,

    @Schema(description = "대여 가능한 수량", example = "7")
    int availableQuantity,

    @Schema(description = "활성 여부", example = "true")
    boolean isActive,

    @Schema(description = "물품 관리 방식", example = "UNIT")
    ItemManagementType itemManagementType,

    @Schema(description = "UNIT 물품일 때의 고유번호 목록")
    List<AdminItemUnitSummary> itemUnits
) {

  public static AdminItemListResponse from(Item item, List<ItemUnit> itemUnits) {
    List<AdminItemUnitSummary> itemUnitSummaries =
        item.isUnitType()
            ? itemUnits.stream().map(AdminItemUnitSummary::from).toList()
            : List.of();

    return new AdminItemListResponse(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getRentalDuration(),
        item.getTotalQuantity(),
        item.getAvailableQuantity(),
        item.isActive(),
        item.getItemManagementType(),
        itemUnitSummaries
    );
  }

  @Schema(description = "UNIT 물품 고유번호 요약")
  public record AdminItemUnitSummary(
      @Schema(description = "고유번호 ID", example = "101")
      Long itemUnitId,
      @Schema(description = "고유번호 코드", example = "NB-001")
      String code,
      @Schema(description = "고유번호 상태", example = "AVAILABLE")
      ItemUnitStatus status
  ) {

    public static AdminItemUnitSummary from(ItemUnit itemUnit) {
      return new AdminItemUnitSummary(
          itemUnit.getId(),
          itemUnit.getCode(),
          itemUnit.getStatus()
      );
    }
  }
}
