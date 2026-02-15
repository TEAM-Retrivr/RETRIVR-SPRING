package retrivr.retrivrspring.presentation.admin.item.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "관리자 물품 등록 요청")
public record AdminItemCreateRequest(

        @Schema(description = "물품명", example = "C타입 충전기")
        @NotBlank
        String name,

        @Schema(description = "물품 설명", example = "고속 충전 지원 충전기")
        String description,

        @Schema(description = "총 수량", example = "10")
        @Min(1)
        int totalQuantity,

        @Schema(description = "대여 가능 기간 (일 단위)", example = "3")
        @Min(1)
        int rentalDuration,

        @Schema(description = "보증물", example = "학생증")
        String guaranteedGoods,

        @Schema(description = "활성 여부", example = "true")
        @NotNull
        Boolean isActive
) {}
