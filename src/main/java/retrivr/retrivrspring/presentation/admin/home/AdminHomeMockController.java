package retrivr.retrivrspring.presentation.admin.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.presentation.admin.home.response.AdminHomeRequestSummary;
import retrivr.retrivrspring.presentation.admin.home.response.AdminHomeResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/home")
@Tag(name = "Admin API / Home", description = "관리자 홈 화면 API")
public class AdminHomeMockController {

    @GetMapping
    @Operation(
            summary = "UC-3.1 관리자 홈 화면 조회",
            description = "대여 요청 미처리 건과 최근 요청 리스트를 반환한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "홈 화면 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = AdminHomeResponse.class)
            )
    )
    public AdminHomeResponse getHome() {

        List<AdminHomeRequestSummary> requests = List.of(
                new AdminHomeRequestSummary(
                        1001L,
                        "8핀 충전기",
                        2,
                        5,
                        "조윤아",
                        "동물자원과학과",
                        "2026-01-21T17:00:00"
                ),
                new AdminHomeRequestSummary(
                        1002L,
                        "8핀 충전기",
                        2,
                        5,
                        "박다솔",
                        "컴퓨터공학과",
                        "2026-01-21T16:30:00"
                )
        );

        return new AdminHomeResponse(
                "건국대학교 도서관자치위원회",
                requests.size(),
                requests
        );
    }
}
