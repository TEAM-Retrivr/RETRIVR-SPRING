package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 비밀번호 재설정 응답")
public record PasswordResetResponse(

        @Schema(description = "이메일", example = "admin@retrivr.com")
        String email,

        @Schema(description = "처리 결과 메시지", example = "비밀번호가 성공적으로 변경되었습니다.")
        String message
) {}
