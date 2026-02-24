package retrivr.retrivrspring.presentation.admin.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import retrivr.retrivrspring.domain.entity.organization.EmailVerificationPurpose;

@Schema(description = "이메일 인증 코드 검증 요청")
public record EmailVerificationRequest(

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(
                description = "인증 목적",
                example = "SIGNUP",
                allowableValues = {"SIGNUP", "PASSWORD_RESET", "LOGIN"}
        )
        @NotNull
        EmailVerificationPurpose purpose,

        @Schema(description = "6자리 인증 코드", example = "123456")
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String code
) {}