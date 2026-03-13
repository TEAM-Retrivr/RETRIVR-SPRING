package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.rental.enumerate.BorrowerFieldType;

@Schema(description = "대여자 입력 요구 항목 응답")
public record AdminItemBorrowerRequirementResponse(
    @Schema(description = "필드 키", example = "name")
    String fieldKey,
    @Schema(description = "필드 라벨", example = "이름")
    String label,
    @Schema(description = "필드 타입", example = "TEXT")
    BorrowerFieldType fieldType,
    @Schema(description = "필수 입력 여부", example = "true")
    boolean required
) {

  public static AdminItemBorrowerRequirementResponse from(ItemBorrowerField itemBorrowerField) {
    return new AdminItemBorrowerRequirementResponse(
        itemBorrowerField.getFieldKey(),
        itemBorrowerField.getLabel(),
        itemBorrowerField.getFieldType(),
        itemBorrowerField.isRequired()
    );
  }
}
