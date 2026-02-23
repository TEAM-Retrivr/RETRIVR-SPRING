package retrivr.retrivrspring.presentation.admin.home.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리자 홈 화면 응답")
public record AdminHomeResponse(

        @Schema(example = "건국대학교 도서관자치위원회")
        String organizationName,

        @Schema(example = "대여 요청 건수")
        int requestCount,

        List<AdminHomeRequestSummary> recentRequests
) {}
