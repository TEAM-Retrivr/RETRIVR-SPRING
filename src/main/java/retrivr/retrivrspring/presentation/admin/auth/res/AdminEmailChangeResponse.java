package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 변경 완료 응답")
public record AdminEmailChangeResponse(
        @Schema(description = "단체 ID", example = "1")
        Long organizationId,

        @Schema(description = "변경 반영된 이메일", example = "user@example.com")
        String email
) {
}
