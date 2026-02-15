package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminSignupResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.EmailVerificationResponse;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Admin Auth")
public class AdminAuthMockController {

    @PostMapping("/login")
    @Operation(summary = "UC-1.1 관리자 로그")
    public AdminLoginResponse login(
            @Valid @RequestBody AdminLoginRequest request
    ) {

        // 🔹 Mock 계정 체크
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
    @Operation(summary = "UC-1.2 관리자 회원가입")
    public AdminSignupResponse signup(
            @Valid @RequestBody AdminSignupRequest request
    ) {

        if ("admin@retrivr.com".equals(request.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 실제 구현
        // 1. organization 생성
        // 2. password → password_hash 변환
        // 3. email_verification row 생성
        // 4. 트랜잭션 commit

        Long mockOrgId = 2L;

        return new AdminSignupResponse(
                mockOrgId,
                request.organizationName(),
                request.email(),
                "PENDING"
        );
    }

    @PostMapping("/email-verification")
    @Operation(summary = "UC-1.3.1 이메일 인증")
    public EmailVerificationResponse verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {

        String mockCode = "123456";

        if (!mockCode.equals(request.code())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        //실제구현
        /*
        1. email_verification 조회
        2. code 일치 여부 확인
        3. expires_at 만료 확인
        4. verified_at 업데이트
        5. organization.status = ACTIVE
         */

        return new EmailVerificationResponse(
                request.email(),
                true,
                java.time.LocalDateTime.now().toString()
        );
    }


}
