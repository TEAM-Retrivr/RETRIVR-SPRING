package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminProfileImageUpdateRequest(
    @Schema(description = "변경한 이미지의 objectKey (nullable - null 일 경우 이미지 삭제)", example = "organizations/%d/profile/%s.%s")
    String objectKey
) {

}
