package retrivr.retrivrspring.presentation.open.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;

public record PhoneVerificationVerifyRequest(
    @NotBlank(message = "verificationId는 필수입니다.")
    String verificationId,
    @NotNull(message = "purpose는 필수입니다.")
    PhoneVerificationPurpose purpose,
    @NotBlank(message = "인증번호는 필수입니다.")
    String rawCode
) {

}
