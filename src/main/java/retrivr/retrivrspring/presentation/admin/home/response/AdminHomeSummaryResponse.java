package retrivr.retrivrspring.presentation.admin.home.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 홈 화면 요약 응답")
public record AdminHomeSummaryResponse(

        @Schema(description = "대여 요청 건수", example = "5")
        int requestedCount,

        @Schema(description = "대여 승인 건수", example = "12")
        int approvedCount,

        @Schema(description = "현재 대여 중 건수", example = "7")
        int rentedCount,

        @Schema(description = "연체 건수", example = "2")
        int overdueCount
) {}
