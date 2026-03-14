package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;

@Schema(description = "대여자 입력 요구 항목 응답")
public record BorrowerRequirementResponse(
    @Schema(description = "필드 라벨", example = "이름")
    @Size(max = 255)
    String label,

    @Schema(description = "필수 입력 여부", example = "true")
    @NotNull
    boolean required
) {

  public static BorrowerRequirementResponse from(ItemBorrowerField itemBorrowerField) {
    return new BorrowerRequirementResponse(
        itemBorrowerField.getLabel(),
        itemBorrowerField.isRequired()
    );
  }
}
