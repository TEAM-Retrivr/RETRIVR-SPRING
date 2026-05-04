package retrivr.retrivrspring.presentation.open.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;

public record PhoneVerificationSendRequest(
    @Schema(
        description = "대여자 전화번호. 숫자, 하이픈(-), 공백, + 입력이 가능합니다.",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(
        regexp = "^[0-9\\-+ ]{7,20}$",
        message = "전화번호 형식이 올바르지 않습니다."
    )
    String phoneNumber,
    @NotNull(message = "purpose는 필수입니다.")
    PhoneVerificationPurpose purpose
) {

}
