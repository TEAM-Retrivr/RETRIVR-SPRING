package retrivr.retrivrspring.presentation.admin.rental.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "대여 승인")
public record AdminRentalApproveRequest(
    @Schema(
        description = "대여 승인을 처리한 관리자 이름",
        example = "김관리",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "관리자 이름은 필수입니다.")
    @Size(max = 100, message = "관리자 이름은 100자 이하로 입력해주세요.")
    String adminNameToApprove
) {

}
