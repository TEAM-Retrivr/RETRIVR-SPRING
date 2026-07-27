package retrivr.retrivrspring.presentation.admin.profile.req;

import jakarta.validation.constraints.NotBlank;

public record AdminProfileUpdateRequest(
        @NotBlank
        String organizationName
) {
}
