package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 유닛 수정 요청")
public record AdminItemUnitChangeRequest(
    @Schema(description = "기존의 unit code", example = "A1K9P2")
    String code,

    @Schema(description = "생성되거나 수정된 unit label", example = "C타입 충전기 파란색")
    String label
) {
}
