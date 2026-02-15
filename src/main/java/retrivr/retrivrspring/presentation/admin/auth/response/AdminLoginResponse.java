package retrivr.retrivrspring.presentation.admin.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(

        @Schema(example = "1")
        Long orgId,

        @Schema(example = "admin@retrivr.com")
        String email,

        @Schema(example = "mock-access-token")
        String accessToken,

        @Schema(example = "mock-refresh-token")
        String refreshToken
) {}
