package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(

        @Schema(example = "1")
        Long organizationId,

        @Schema(example = "admin@retrivr.com")
        String email,

        @Schema(example = "mock-access-token")
        String accessToken,

        @Schema(example = "mock-refresh-token")
        String refreshToken
) {}
