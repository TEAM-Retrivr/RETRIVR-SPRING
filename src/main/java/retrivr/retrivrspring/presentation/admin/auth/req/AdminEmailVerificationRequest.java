package retrivr.retrivrspring.presentation.admin.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "관리자 이메일 변경 인증 코드 검증 요청")
public record AdminEmailVerificationRequest(
        @Schema(description = "변경할 이메일", example = "new@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "6자리 이메일 인증 코드", example = "123456")
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String code,

        @Schema(
                description = "EMAIL_CHANGE 목적으로 발급받은 비밀번호 인증 토큰",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank
        String passwordVerificationToken
) {
}
