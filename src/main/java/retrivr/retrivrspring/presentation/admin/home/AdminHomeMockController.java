package retrivr.retrivrspring.presentation.admin.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.home.AdminHomeService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.home.res.AdminHomeResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/home")
@Tag(name = "Admin API / Home", description = "관리자 홈 화면 API")
public class AdminHomeMockController {

    private final AdminHomeService adminHomeService;

    @GetMapping
    @Operation(
            summary = "UC-3.1 관리자 홈 화면 조회",
            description = "대여 요청 미처리 건과 최근 요청 2건 리스트를 반환한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "홈 화면 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = AdminHomeResponse.class)
            )
    )
    public AdminHomeResponse getHome(@AuthOrg AuthUser authUser) {
        return adminHomeService.getHome(authUser.organizationId());
    }
}