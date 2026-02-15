package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminSignupRequest;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminLoginResponse;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminSignupResponse;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Admin Auth")
public class AdminAuthMockController {

    @PostMapping("/login")
    @Operation(summary = "UC-1.1 관리자 로그인 (Mock)")
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
    @Operation(summary = "UC-1.2 관리자 회원가입 (Mock)")
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


}
