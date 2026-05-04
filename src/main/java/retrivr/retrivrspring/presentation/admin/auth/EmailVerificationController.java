package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

@RestController
@RequestMapping("/api/public/v1/email/verification")
@Tag(name = "Public API / Email Verification", description = "이메일 인증 관련 API")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping
    @Operation(
            summary = "UC-1.3.0 이메일 인증 코드 발송",
            description = "이메일과 인증 목적에 따라 6자리 인증 코드를 발송한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "인증 코드 발송 성공",
            content = @Content(schema = @Schema(implementation = EmailVerificationSendResponse.class))
    )
    @ApiErrorCodeExamples({ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS})
    public ResponseEntity<EmailVerificationSendResponse> sendEmailVerificationCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        return ResponseEntity.ok(
                emailVerificationService.sendCode(request)
        );
    }

    @Operation(
            summary = "UC-1.3.1 이메일 인증 코드 검증",
            description = "이메일, 목적, 인증 코드를 검증하고 인증을 완료한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "이메일 인증 성공",
            content = @Content(schema = @Schema(implementation = EmailCodeVerifyTokenResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.EMAIL_VERIFICATION_NOT_FOUND,
            ErrorCode.EMAIL_VERIFICATION_EXPIRED,
            ErrorCode.EMAIL_ALREADY_VERIFIED,
            ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH
    })
    @PostMapping("/verify")
    public ResponseEntity<EmailCodeVerifyTokenResponse> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }
}
