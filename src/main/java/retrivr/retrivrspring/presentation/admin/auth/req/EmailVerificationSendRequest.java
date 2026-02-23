package retrivr.retrivrspring.presentation.admin.auth.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationSendRequest(

        @Email
        @NotBlank
        String email
) {}