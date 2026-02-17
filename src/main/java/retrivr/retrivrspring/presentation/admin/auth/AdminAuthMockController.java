package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.PasswordResetRequest;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.EmailVerificationResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.PasswordResetResponse;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Admin API / Auth", description = "관리자 인증 관련 API")
public class AdminAuthMockController {

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
    public AdminLoginResponse login(
            @Valid @RequestBody AdminLoginRequest request
    ) {

        if (!"admin@retrivr.com".equals(request.email())
                || !"password1234".equals(request.password())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        Long mockOrgId = 1L;

        return new AdminLoginResponse(
                mockOrgId,
                request.email(),
                "mock-access-token",
                "mock-refresh-token"
        );
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

        if ("admin@retrivr.com".equals(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Long mockOrgId = 2L;

        return new AdminSignupResponse(
                mockOrgId,
                request.organizationName(),
                request.email(),
                "PENDING"
        );
    }

    @PostMapping("/email-verification")
    @Operation(
            summary = "UC-1.3.1 이메일 인증",
            description = "이메일로 발급된 인증 코드를 검증하고 인증을 완료한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "이메일 인증 성공",
            content = @Content(schema = @Schema(implementation = EmailVerificationResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "인증 코드 불일치 또는 만료"
    )
    public EmailVerificationResponse verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {

        String mockCode = "123456";

        if (!mockCode.equals(request.code())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        return new EmailVerificationResponse(
                request.email(),
                true,
                java.time.LocalDateTime.now()
        );
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
    @ApiResponse(
            responseCode = "400",
            description = "비밀번호 확인 불일치 또는 정책 위반"
    )
    public PasswordResetResponse resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("비밀번호 확인 값이 일치하지 않습니다.");
        }

        // 🔹 Mock 정책 검증 (길이 체크)
        if (request.newPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        return new PasswordResetResponse(
                request.email(),
                "비밀번호가 성공적으로 변경되었습니다."
        );
    }


}
