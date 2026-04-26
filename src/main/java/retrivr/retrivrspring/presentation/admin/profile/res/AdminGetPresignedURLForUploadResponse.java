package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminGetPresignedURLForUploadResponse(
    @Schema(description = "현재 로그인된 조직 ID", example = "10")
    Long organizationId,
    @Schema(description = "S3 업로드용 Presigned URL", example = "https://s3.retrivr/image")
    String uploadUrl
) {

}
