package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.auth.request.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.response.AdminLoginResponse;

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
}
