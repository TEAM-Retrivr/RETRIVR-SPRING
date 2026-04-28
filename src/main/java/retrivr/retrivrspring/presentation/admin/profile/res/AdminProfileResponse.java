package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 프로필 조회/수정 응답")
public record AdminProfileResponse(
        @Schema(description = "단체 명", example = "건국대학교 전산원")
        String organizationName,

        Long organizationId,

        @Schema(
                description = "단체 이미지 URL",
                example = "https://cdn.retrivr.com/organizations/5/profile/uuid.png"
        )
        String profileImageUrl,

        @Schema(description = "단체 이메일", example = "admin@retrivr.com")
        String email
) {
}
