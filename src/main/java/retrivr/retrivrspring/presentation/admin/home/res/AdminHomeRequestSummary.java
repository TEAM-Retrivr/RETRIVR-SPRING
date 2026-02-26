package retrivr.retrivrspring.presentation.admin.home.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자 홈 화면 대여 요청 카드")
public record AdminHomeRequestSummary(

        @Schema(description = "대여 ID", example = "1001")
        Long rentalId,

        @Schema(description = "물품명", example = "8핀 충전기")
        String itemName,

        @Schema(description = "현재 대여 가능 수량", example = "2")
        int availableQuantity,

        @Schema(description = "총 수량", example = "5")
        int totalQuantity,

        @Schema(description = "대여자 이름", example = "홍길동")
        String borrowerName,

        @Schema(description = "학과", example = "감귤포장학과")
        String borrowerMajor,

        @Schema(description = "요청 일시", example = "2026-01-21T17:00:00")
        LocalDateTime requestedAt
) {}