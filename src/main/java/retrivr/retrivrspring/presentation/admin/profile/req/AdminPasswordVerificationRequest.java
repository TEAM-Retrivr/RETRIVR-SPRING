package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;

public record AdminPasswordVerificationRequest(
        @Schema(description = "현재 비밀번호", example = "Password123!")
        @NotBlank
        String password,

        @Schema(
                description = "재인증 목적",
                allowableValues = {"EMAIL_CHANGE", "PASSWORD_CHANGE", "ADMIN_CODE_CHANGE"}
        )
        @NotNull
        PasswordVerificationPurpose purpose
) {
}
