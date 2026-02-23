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
import retrivr.retrivrspring.presentation.admin.auth.req.*;
import retrivr.retrivrspring.presentation.admin.auth.res.*;

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

    @PostMapping("/signup/email-code/send")
    @Operation(
            summary = "UC-1.3.1-1 회원가입용 이메일 인증 코드 발송",
            description = "입력된 이메일이 가입되어 있지 않으면 6자리 인증 코드를 발송한다. 코드는 10분간 유효하다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "인증 코드 발송 성공",
            content = @Content(schema = @Schema(implementation = EmailCodeSendResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "이미 가입된 이메일/잘못된 요청")
    public EmailCodeSendResponse sendSignupEmailCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        return adminAuthService.sendSignupEmailCode(request);
    }

    @PostMapping("/signup/email-code/verify")
    @Operation(
            summary = "UC-1.3.1-2 회원가입용 이메일 인증 코드 검증",
            description = "이메일과 인증 코드를 검증한다. 성공 시 회원가입에만 사용하는 signupToken을 발급한다. 토큰은 10분간 유효하다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "이메일 인증 성공",
            content = @Content(schema = @Schema(implementation = EmailCodeVerifyResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "코드 불일치/만료/이미 사용됨")
    public EmailCodeVerifyResponse verifySignupEmailCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return adminAuthService.verifySignupEmailCode(request);
    }
}