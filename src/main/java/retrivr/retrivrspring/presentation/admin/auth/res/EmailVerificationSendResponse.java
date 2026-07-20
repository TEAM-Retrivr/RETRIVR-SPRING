package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 코드 발송 응답")
public record EmailVerificationSendResponse(

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "인증 목적", example = "SIGNUP")
        String purpose,

        @Schema(description = "인증 코드 유효 시간(초). 응답 수신 시점부터 이 시간이 지나면 코드가 만료된다.", example = "600")
        long expiresInSeconds,

        @Schema(
                description = "재발송 가능해질 때까지 남은 시간(초). 이 시간이 지나기 전에 재발송하면 429(7104)로 거부된다.",
                example = "60"
        )
        long resendAvailableInSeconds
) {}