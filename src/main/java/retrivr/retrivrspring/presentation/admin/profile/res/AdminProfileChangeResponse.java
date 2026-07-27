package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminProfileChangeResponse(
        @Schema(description = "변경 성공 여부", example = "true")
        boolean success
) {
    public static AdminProfileChangeResponse ofSuccess() {
        return new AdminProfileChangeResponse(true);
    }
}
