package retrivr.retrivrspring.presentation.admin.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerificationResponse(

        @Schema(example = "admin@retrivr.com")
        String email,

        @Schema(example = "true")
        boolean verified,

        @Schema(example = "2026-02-15T12:00:00")
        String verifiedAt
) {}
