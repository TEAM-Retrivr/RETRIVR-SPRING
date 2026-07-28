package retrivr.retrivrspring.presentation.admin.profile.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminPasswordVerificationResponse(
        @Schema(
                description = "개인정보 변경에 사용할 일회용 인증 토큰",
                example = "pvt_550e8400-e29b-41d4-a716-446655440000"
        )
        String verificationToken,

        @Schema(description = "인증 토큰 유효 시간(초)", example = "300")
        long expiresIn
) {
}
