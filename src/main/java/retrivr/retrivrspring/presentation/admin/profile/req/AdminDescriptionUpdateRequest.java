package retrivr.retrivrspring.presentation.admin.profile.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "대여지 설명 입력/수정 요청")
public record AdminDescriptionUpdateRequest(
    @Schema(description = "대여지 설명. 빈 문자열로 삭제할 수 있습니다.", maxLength = 200)
    @NotNull
    @Size(max = 200)
    String description
) {}
