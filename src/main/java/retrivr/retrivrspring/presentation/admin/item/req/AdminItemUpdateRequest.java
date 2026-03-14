package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

@Schema(description = "관리자 물품 수정 요청")
public record AdminItemUpdateRequest(
    @Schema(description = "물품명", example = "C타입 고속 충전기")
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(description = "물품 설명. null이면 설명을 제거합니다.", example = "고속 충전 지원 충전기")
    @Size(max = 2000)
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "5")
    @NotNull
    @Positive
    Integer rentalDuration,

    @Schema(description = "활성 여부", example = "true")
    @NotNull
    Boolean isActive,

    @Schema(description = "물품 관리 방식", example = "UNIT")
    @NotNull
    ItemManagementType itemManagementType,

    @Schema(description = "대여자 입력 요구 정보 목록. null이면 기본 preset 필드로 재설정됩니다.")
    @Valid
    List<BorrowerRequirementRequest> borrowerRequirements,

    @Schema(description = "활성 여부", example = "true")
    @NotNull
    Boolean isActive
) {
}
