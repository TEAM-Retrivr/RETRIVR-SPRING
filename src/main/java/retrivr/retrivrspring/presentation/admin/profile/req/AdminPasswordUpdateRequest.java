package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminPasswordUpdateRequest(
        @Schema(description = "새 비밀번호", example = "NewPassword123!")
        @NotBlank
        String newPassword,

        @Schema(description = "새 비밀번호 확인", example = "NewPassword123!")
        @NotBlank
        String confirmPassword,

        @Schema(description = "PASSWORD_CHANGE 목적으로 발급받은 비밀번호 인증 토큰")
        @NotBlank
        String passwordVerificationToken
) {
}
