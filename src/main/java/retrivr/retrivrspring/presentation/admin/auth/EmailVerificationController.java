package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.application.service.EmailVerificationService;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationResponse;

@RestController
@RequestMapping("/api/public/v1/admin/auth")
@Tag(name = "Public API / Email Verification", description = "이메일 인증 관련 API")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email/send")
    @Operation(
            summary = "UC-1.3.0 이메일 인증 코드 발송",
            description = "관리자 이메일로 6자리 인증 코드를 발송한다."
    )
    @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공")
    @ApiResponse(responseCode = "400", description = "이메일 오류")
    public void sendEmailVerificationCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        emailVerificationService.sendCode(request.email());
    }

    @Operation(
            summary = "UC-1.3.1 이메일 인증 코드 검증",
            description = "이메일로 발급된 인증 코드를 검증하고 인증을 완료한다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = EmailVerificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이메일")
    })
    @PostMapping("/email/verify")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "이메일 인증 코드 검증 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmailVerificationRequest.class))
            )
            EmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }
}