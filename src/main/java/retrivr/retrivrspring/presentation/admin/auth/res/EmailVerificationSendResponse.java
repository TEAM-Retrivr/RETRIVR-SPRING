package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 코드 발송 응답")
public record EmailVerificationSendResponse(

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "인증 목적", example = "SIGNUP")
        String purpose,

        @Schema(description = "유효 시간(초)", example = "600")
        long expiresInSeconds
) {}