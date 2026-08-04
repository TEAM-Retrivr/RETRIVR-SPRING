package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 단체명 수정 요청")
public record AdminProfileUpdateRequest(
        @Schema(description = "변경할 단체명", example = "건국대학교 전산원")
        @NotBlank
        String organizationName
) {
}
