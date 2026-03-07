package retrivr.retrivrspring.presentation.admin.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;

@Schema(description = "관리자 비밀번호 재설정 요청")
public record PasswordResetRequest(

        @Schema(description = "관리자 이메일", example = "admin@retrivr.com")
        @Email
        @NotBlank
        String email,

        @Schema(
                description = "Verification purpose",
                example = "PASSWORD_RESET",
                allowableValues = {"PASSWORD_RESET"}
        )
        @NotNull
        EmailVerificationPurpose purpose,

        @Schema(description = "비밀번호 재설정 토큰", example = "a8f3k2l9m1n0p4q6r7s8t9u0v1w2x3y4")
        @NotBlank
        String token,

        @Schema(description = "새 비밀번호", example = "newPassword123!")
        @NotBlank
        String newPassword,

        @Schema(description = "비밀번호 확인", example = "newPassword123!")
        @NotBlank
        String confirmPassword
) {}
