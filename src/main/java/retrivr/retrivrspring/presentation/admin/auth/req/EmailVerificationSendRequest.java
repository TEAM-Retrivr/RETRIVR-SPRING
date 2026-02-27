package retrivr.retrivrspring.presentation.admin.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;

@Schema(description = "이메일 인증 코드 발송 요청")
public record EmailVerificationSendRequest(

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
        EmailVerificationPurpose purpose
) {}