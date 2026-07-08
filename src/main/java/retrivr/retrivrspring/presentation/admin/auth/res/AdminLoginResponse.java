package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(

        @Schema(example = "1")
        Long organizationId,

        @Schema(example = "admin@retrivr.com")
        String email,

        @Schema(example = "mock-access-token")
        String accessToken
) {
    public static AdminLoginResponse of(Long organizationId, String email, String accessToken) {
        return new AdminLoginResponse(organizationId, email, accessToken);
    }
}
