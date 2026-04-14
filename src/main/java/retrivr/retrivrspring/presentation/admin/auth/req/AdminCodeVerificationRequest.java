package retrivr.retrivrspring.presentation.admin.auth.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;

public record AdminCodeVerificationRequest(
    @Pattern(regexp = "\\d{6}", message = "관리자 코드는 6자리 숫자여야 합니다.")
    String adminCode,
    @NotNull
    AdminCodeVerificationPurpose purpose
) {

}
