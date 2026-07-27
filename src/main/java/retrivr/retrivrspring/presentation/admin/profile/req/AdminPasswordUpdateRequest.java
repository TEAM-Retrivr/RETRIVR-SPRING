package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AdminPasswordUpdateRequest(
        @Schema(
                description = "새 비밀번호 (영문, 숫자, 특수문자를 포함한 8자 이상)",
                example = "NewPassword123!"
        )
        @NotBlank
        String newPassword,

        @Schema(
                description = "새 비밀번호 확인 (newPassword와 동일한 값)",
                example = "NewPassword123!"
        )
        @NotBlank
        String confirmPassword,

        @Schema(
                description = "PASSWORD_CHANGE 목적으로 발급받은 비밀번호 인증 토큰",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank
        String passwordVerificationToken
) {
}
