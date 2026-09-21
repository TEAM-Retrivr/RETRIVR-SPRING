package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 유닛 수정 요청")
public record AdminItemUnitChangeRequest(
    @Schema(description = "수정 또는 삭제할 unit ID. 신규 생성 시에는 전달하지 않습니다.", example = "101")
    Long itemUnitId,

    @Schema(description = "생성 또는 변경할 unit label", example = "충전기 2")
    String label
) {
}
