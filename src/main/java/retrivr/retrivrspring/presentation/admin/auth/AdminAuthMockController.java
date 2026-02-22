package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.application.service.AdminAuthService;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.PasswordResetResponse;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Admin API / Auth", description = "관리자 인증 관련 API")
@RequiredArgsConstructor
public class AdminAuthMockController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(
            summary = "UC-1.1 관리자 로그인",
            description = "이메일과 비밀번호를 입력받아 관리자 인증 후 access/refresh 토큰을 발급한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = AdminLoginResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "이메일 또는 비밀번호 불일치"
    )
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }

    @PostMapping("/signup")
    @Operation(
            summary = "UC-1.2 관리자 회원가입",
            description = "이메일, 비밀번호, 단체명을 입력받아 새로운 관리자 계정을 생성한다. 생성 후 이메일 인증이 필요하다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = AdminSignupResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "이미 가입된 이메일 또는 입력값 오류"
    )
    public AdminSignupResponse signup(
            @Valid @RequestBody AdminSignupRequest request
    ) {

        return adminAuthService.signup(request);
    }


    @PatchMapping("/password")
    @Operation(
            summary = "UC-1.3.2 관리자 비밀번호 재설정",
            description = "이메일 인증 완료 후 비밀번호를 변경한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 변경 성공",
            content = @Content(schema = @Schema(implementation = PasswordResetResponse.class))
    )
    public PasswordResetResponse resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return adminAuthService.resetPassword(request);
    }


}
