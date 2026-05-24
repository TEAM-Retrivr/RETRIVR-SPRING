package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.auth.AdminAuthService;
import retrivr.retrivrspring.application.service.admin.auth.AdminLoginResult;
import retrivr.retrivrspring.application.service.admin.auth.AdminLogoutResult;
import retrivr.retrivrspring.application.service.admin.auth.AdminRefreshResult;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.PasswordResetRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLogoutResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.PasswordResetSuccessResponse;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Admin API / Auth", description = "관리자 인증 관련 API")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminRefreshTokenCookieManager refreshTokenCookieManager;
    private final AdminAuthOriginValidator adminAuthOriginValidator;

    @PostMapping("/login")
    @Operation(
            summary = "UC-1.1 관리자 로그인",
            description = "이메일과 비밀번호를 입력받아 관리자 인증 후 access token을 발급하고 refresh token을 HttpOnly 쿠키에 저장한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = AdminLoginResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_CREDENTIALS,
            ErrorCode.ACCOUNT_SUSPENDED,
            ErrorCode.ACCOUNT_NOT_APPROVED
    })
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResult loginResult = adminAuthService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(loginResult.refreshToken()).toString())
                .body(AdminLoginResponse.of(
                        loginResult.organizationId(),
                        loginResult.email(),
                        loginResult.accessToken()
                ));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "UC-1.1.1 Access Token 재발급",
            description = "쿠키에 저장된 refresh token을 검증한 뒤 새로운 access token을 발급한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Access Token 재발급 성공",
            content = @Content(schema = @Schema(implementation = AdminLoginResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.ACCOUNT_NOT_FOUND,
            ErrorCode.ACCOUNT_SUSPENDED,
            ErrorCode.ACCOUNT_NOT_APPROVED
    })
    public AdminLoginResponse refresh(HttpServletRequest request) {
        adminAuthOriginValidator.validate(request);
        String refreshToken = refreshTokenCookieManager.extract(request);
        AdminRefreshResult refreshResult = adminAuthService.refresh(refreshToken);
        return AdminLoginResponse.of(
                refreshResult.organizationId(),
                refreshResult.email(),
                refreshResult.accessToken()
        );
    }

    @PostMapping("/signup")
    @Operation(
            summary = "UC-1.2 관리자 회원가입",
            description = "이메일, 비밀번호, 단체명을 입력받아 새로운 관리자 계정을 생성한다. 계정 생성 전 가입 토큰 검증이 필요하다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = AdminSignupResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.SIGNUP_TOKEN_NOT_FOUND,
            ErrorCode.SIGNUP_TOKEN_INVALID,
            ErrorCode.SIGNUP_TOKEN_EXPIRED,
            ErrorCode.SIGNUP_TOKEN_ALREADY_USED,
            ErrorCode.INVALID_VALUE_EXCEPTION,
            ErrorCode.ALREADY_EXIST_EXCEPTION
    })
    public AdminSignupResponse signup(@Valid @RequestBody AdminSignupRequest request) {
        return adminAuthService.signup(request);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "UC-1.1.2 관리자 로그아웃",
            description = "현재 refresh token을 삭제하고 refresh token 쿠키를 만료시킨다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(schema = @Schema(implementation = AdminLogoutResponse.class))
    )
    public ResponseEntity<AdminLogoutResponse> logout(HttpServletRequest request) {
        adminAuthOriginValidator.validate(request);
        String refreshToken = refreshTokenCookieManager.extract(request);
        AdminLogoutResult result = adminAuthService.logout(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.delete().toString())
                .body(new AdminLogoutResponse(result.success()));
    }

    @PatchMapping("/password")
    @Operation(
            summary = "UC-1.3.2 관리자 비밀번호 재설정",
            description = "이메일 인증 완료 후 비밀번호를 변경한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 변경 성공",
            content = @Content(schema = @Schema(implementation = PasswordResetSuccessResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_VALUE_EXCEPTION,
            ErrorCode.PASSWORD_RESET_POLICY_VIOLATION,
            ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH,
            ErrorCode.ACCOUNT_NOT_FOUND,
            ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND,
            ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED,
            ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED,
            ErrorCode.PASSWORD_RESET_TOKEN_INVALID
    })
    public PasswordResetSuccessResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return adminAuthService.resetPassword(request);
    }
}
