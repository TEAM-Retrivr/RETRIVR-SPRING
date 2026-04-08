package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AdminGetPresignedURLForUploadRequest(
    @Schema(description = "변경할 이미지 컨텐츠 타입. (허용목록: image/jpeg, image/jpg, image/png, image/webp", example = "image/jpg")
    @NotNull
    String imageContentType
) {

}