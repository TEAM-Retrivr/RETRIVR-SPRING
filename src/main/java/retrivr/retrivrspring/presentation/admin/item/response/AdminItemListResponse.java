package retrivr.retrivrspring.presentation.admin.item.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 물품 목록 응답")
public record AdminItemListResponse(

        @Schema(example = "11")
        Long itemId,

        @Schema(example = "노트북")
        String name,

        @Schema(example = "10")
        int totalQuantity,

        @Schema(example = "7")
        int availableQuantity,

        @Schema(example = "true")
        boolean isActive
) {}
