package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 프로필 조회/수정 응답")
public record AdminProfileResponse(
        @Schema(description = "단체 명", example = "건국대학교 전산원")
        String organizationName,

        @Schema(description = "단체 ID", example = "1")
        Long organizationId,

        @Schema(description = "단체 이메일", example = "admin@retrivr.com")
        String email,

        @Schema(
                description = "단체 프로필 이미지 URL",
                example = "https://s3.retrivr/organizations/1/profile/image.png",
                nullable = true
        )
        String profileImageUrl
) {
}
