package retrivr.retrivrspring.presentation.admin.home.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 홈 화면 대여 요청 카드")
public record AdminHomeRequestSummary(

        @Schema(example = "1001")
        Long rentalId,

        @Schema(example = "8핀 충전기")
        String itemName,

        @Schema(example = "2")
        int availableQuantity,

        @Schema(example = "5")
        int totalQuantity,

        @Schema(example = "조윤아")
        String borrowerName,

        @Schema(example = "동물자원과학과")
        String borrowerMajor,

        @Schema(example = "2026-01-21T17:00:00")
        String requestedAt
) {}
