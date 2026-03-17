package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "대여자 입력 요구 항목")
public record BorrowerRequirementRequest(
    @Schema(description = "필드 라벨", example = "학번")
    @Size(max = 255)
    String label,

    @Schema(description = "필수 입력 여부", example = "true")
    @NotNull
    boolean required
) {
}
