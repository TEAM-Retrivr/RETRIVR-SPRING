package retrivr.retrivrspring.presentation.admin.item.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "관리자 물품 수정 요청")
public record AdminItemUpdateRequest(

        @Schema(description = "물품명", example = "C타입 고속 충전기")
        String name,

        @Schema(description = "물품 설명", example = "65W 고속 충전 지원")
        String description,

        @Schema(description = "총 수량", example = "15")
        @Min(1)
        Integer totalQuantity,

        @Schema(description = "대여 가능 기간 (일)", example = "2")
        @Min(1)
        Integer rentalDuration,

        @Schema(description = "보증물", example = "학생증")
        String guaranteedGoods,

        @Schema(description = "활성 여부", example = "true")
        Boolean isActive
) {}
