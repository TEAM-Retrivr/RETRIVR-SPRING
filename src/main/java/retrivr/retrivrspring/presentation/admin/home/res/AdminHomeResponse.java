package retrivr.retrivrspring.presentation.admin.home.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리자 홈 화면 응답")
public record AdminHomeResponse(

        @Schema(description = "단체명", example = "건국대학교 도서관자치위원회")
        String organizationName,

        @Schema(
                description = "단체 프로필 이미지 URL",
                example = "https://cdn.retrivr.com/organizations/5/profile/uuid.png"
        )
        String profileImageUrl,

        @Schema(description = "승인 대기 요청 전체 건수", example = "7")
        int requestCount,

        @Schema(description = "최근 대여 요청 2건")
        List<AdminHomeRequestSummary> recentRequests
) {}