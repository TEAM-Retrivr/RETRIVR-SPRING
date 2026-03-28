package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;

import java.util.List;

@Schema(description = "관리자 물품 등록 요청")
public record AdminItemCreateRequest(
    @Schema(description = "물품명", example = "C타입 충전기")
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(description = "물품 설명", example = "고속 충전 지원 충전기")
    @Size(max = 2000)
    String description,

    @Schema(description = "대여 가능 기간(일)", example = "3")
    @NotNull
    @Positive
    Integer rentalDuration,

    @Schema(description = "물품 총 개수(개)", example = "3")
    @NotNull
    @Positive
    Integer totalQuantity,

    @Schema(description = "물품 관리 방식", example = "NON_UNIT")
    @NotNull
    ItemManagementType itemManagementType,

    @Schema(description = "독촉 문자 발송 여부", example = "true")
    @NotNull
    Boolean useMessageAlarmService,

    @Schema(description = "담보물품 여부 및 종류", example = "학생증")
    String guaranteedGoods,

    @Schema(description = "유닛 라벨 목록", example = "[\"검은색 충전기\", \"흰색 충전기\"]")
    List<String> unitLabels,

    @Schema(description = "대여자 입력 요구 정보 목록")
    @Valid
    List<BorrowerRequirementRequest> borrowerRequirements
) {
}
