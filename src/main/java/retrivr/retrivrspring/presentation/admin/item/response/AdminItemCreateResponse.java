package retrivr.retrivrspring.presentation.admin.item.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 등록 응답")
public record AdminItemCreateResponse(

        @Schema(example = "101")
        Long itemId,

        @Schema(example = "C타입 충전기")
        String name,

        @Schema(example = "10")
        int totalQuantity
) {}
