package retrivr.retrivrspring.presentation.admin.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.presentation.admin.home.response.AdminHomeSummaryResponse;

@RestController
@RequestMapping("/api/admin/v1/home")
@Tag(name = "Admin API / Home", description = "관리자 홈 화면 요약 API")
public class AdminHomeMockController {

    @GetMapping
    @Operation(
            summary = "UC-3.1 관리자 홈 화면 요약 조회",
            description = "대여 요청, 승인, 대여 중, 연체 건수를 집계하여 반환한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "홈 화면 요약 조회 성공",
            content = @Content(schema = @Schema(implementation = AdminHomeSummaryResponse.class))
    )
    public AdminHomeSummaryResponse getHomeSummary() {

        // Mock 집계 값
        return new AdminHomeSummaryResponse(
                5,   // REQUESTED
                12,  // APPROVED
                7,   // RENTED
                2    // OVERDUE
        );
    }
}
