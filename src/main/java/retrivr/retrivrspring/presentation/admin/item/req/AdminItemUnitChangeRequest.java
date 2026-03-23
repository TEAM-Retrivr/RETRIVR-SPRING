package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 유닛 수정 요청")
public record AdminItemUnitChangeRequest(
    @Schema(description = "기존 unit label", example = "충전기 1")
    String currentLabel,

    @Schema(description = "생성 또는 변경할 unit label", example = "충전기 2")
    String label
) {
}
