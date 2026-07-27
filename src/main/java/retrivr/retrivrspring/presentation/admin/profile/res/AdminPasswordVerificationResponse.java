package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminPasswordVerificationResponse(
        @Schema(description = "개인정보 변경에 사용할 일회용 인증 토큰")
        String verificationToken,

        @Schema(description = "인증 토큰 유효 시간(초)", example = "300")
        long expiresIn
) {
}
