package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminProfileUpdateRequest(
        @Email
        @NotBlank
        String newEmail,

        @NotBlank
        String newPassword,

        @NotBlank
        String confirmPassword,

        @NotBlank
        String newOrganizationName,

        @NotBlank
        String newAdminCode,

        @NotBlank
        @Schema(description = "관리자 인증 번호 검증 후 받은 토큰", example = "cvt_token")
        String adminCodeVerificationToken
) {
}
