package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "UNIT 물품 고유번호 대여 가능 여부 변경 요청")
public record AdminItemUnitAvailabilityUpdateRequest(
    @Schema(description = "true면 대여 가능, false면 대여 불가", example = "false")
    @NotNull
    Boolean isAvailable
) {
}
