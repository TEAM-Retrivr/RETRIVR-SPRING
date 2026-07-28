package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 이메일 변경 인증 코드 발송 응답")
public record AdminEmailVerificationSendResponse(
        @Schema(description = "인증 코드 유효 시간(초)", example = "600")
        long expiresInSeconds,

        @Schema(description = "재발송 가능해질 때까지 남은 시간(초)", example = "60")
        long resendAvailableInSeconds
) {
    public static AdminEmailVerificationSendResponse from(
            EmailVerificationSendResponse response
    ) {
        return new AdminEmailVerificationSendResponse(
                response.expiresInSeconds(),
                response.resendAvailableInSeconds()
        );
    }
}
