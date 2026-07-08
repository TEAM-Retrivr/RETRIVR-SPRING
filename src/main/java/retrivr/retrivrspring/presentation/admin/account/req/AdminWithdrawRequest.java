package retrivr.retrivrspring.presentation.admin.account.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import retrivr.retrivrspring.domain.entity.organization.enumerate.WithdrawalReasonCode;

public record AdminWithdrawRequest(

        @NotBlank
        @Schema(description = "탈퇴 전 최종 확인용 비밀번호")
        String password,

        @NotNull
        @Schema(description = "다중 선택 가능한 탈퇴 사유 코드 목록")
        List<WithdrawalReasonCode> reasonCodes,

        @Size(max = 200)
        @Schema(description = "OTHER 선택 시 입력하는 상세 사유", nullable = true)
        String otherReason,

        @NotNull
        @AssertTrue
        @Schema(description = "탈퇴 유의사항 동의 여부", example = "true")
        Boolean agreedToWarning
) {
}
