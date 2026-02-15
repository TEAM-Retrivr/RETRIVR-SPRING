package retrivr.retrivrspring.presentation.admin.item.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 수정 응답")
public record AdminItemUpdateResponse(

        @Schema(example = "101")
        Long itemId,

        @Schema(example = "C타입 고속 충전기")
        String name,

        @Schema(example = "15")
        int totalQuantity,

        @Schema(example = "true")
        boolean isActive
) {}
