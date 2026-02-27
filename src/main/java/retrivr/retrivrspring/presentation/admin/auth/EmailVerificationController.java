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
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationResponse;
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 발송 성공",
                    content = @Content(schema = @Schema(implementation = EmailVerificationSendResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = EmailVerificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이메일")
    })
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }
}