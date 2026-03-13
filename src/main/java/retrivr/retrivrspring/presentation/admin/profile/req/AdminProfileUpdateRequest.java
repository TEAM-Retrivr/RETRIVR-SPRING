package retrivr.retrivrspring.presentation.admin.profile.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
        String newAdminCode
) {
}
