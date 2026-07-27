package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminCodeUpdateRequest(
        @Schema(description = "새 관리자 코드", example = "123456")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "관리자 코드는 숫자 6자리여야 합니다.")
        String newAdminCode,

        @Schema(description = "새 관리자 코드 확인", example = "123456")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "관리자 코드 확인은 숫자 6자리여야 합니다.")
        String confirmAdminCode,

        @Schema(description = "ADMIN_CODE_CHANGE 목적으로 발급받은 비밀번호 인증 토큰")
        @NotBlank
        String passwordVerificationToken
) {
}
