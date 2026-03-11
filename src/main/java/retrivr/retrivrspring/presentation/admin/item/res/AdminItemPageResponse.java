package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리자 물품 목록 커서 페이지 응답")
public record AdminItemPageResponse(
    @Schema(description = "물품 목록")
    List<AdminItemListResponse> items,
    @Schema(description = "다음 페이지 조회용 커서. 마지막 페이지면 null", example = "12")
    Long nextCursor
) {
}
