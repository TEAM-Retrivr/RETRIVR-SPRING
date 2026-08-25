package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminEmailVerificationSendResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

@RestController
@RequestMapping("/api")
@Tag(name = "Email Verification", description = "이메일 인증 관련 API")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final AdminRefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/public/v1/email/verification")
    @Operation(
            summary = "public 이메일 인증 코드 발송",
            description = "이메일과 인증 목적에 따라 6자리 인증 코드를 발송한다. "
                    + "SIGNUP, PASSWORD_RESET, BORROW 목적만 허용한다. "
                    + "재발송 대기 중이면 429(7104)를 반환하며, 이때 detail 에 남은 대기 시간(초)이 담긴다."
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
        return ResponseEntity.ok(emailVerificationService.sendPublicCode(request));
    }

    @PostMapping("/public/v1/email/verification/verify")
    @Operation(
            summary = "public 이메일 인증 코드 검증",
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
    public ResponseEntity<EmailCodeVerifyTokenResponse> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }

    @PostMapping("/admin/v1/email/verification")
    @Operation(
            summary = "admin 이메일 인증 코드 발송",
            description = "관리자 정보 수정 시 이메일 변경을 위해 6자리 인증 코드를 발송한다. "
                    + "현재 사용 중인 이메일이거나 다른 단체가 이미 사용 중인 이메일이면 발송하지 않는다. "
                    + "재발송 대기 중이면 429(7104)를 반환하며, 이때 detail 에 남은 대기 시간(초)이 담긴다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "인증 코드 발송 성공",
            content = @Content(schema = @Schema(implementation = AdminEmailVerificationSendResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS,
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.EMAIL_SAME_AS_CURRENT,
            ErrorCode.ALREADY_EXIST_EXCEPTION,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
    })
    public ResponseEntity<AdminEmailVerificationSendResponse> sendAdminEmailVerificationCode(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminEmailVerificationSendRequest request
    ) {
        EmailVerificationSendResponse response =
                emailVerificationService.sendChangeEmailCode(request, authUser.organizationId());
        return ResponseEntity.ok(AdminEmailVerificationSendResponse.from(response));
    }

    @PostMapping("/admin/v1/email/verification/verify")
    @Operation(
            summary = "admin 이메일 인증 코드 검증 및 이메일 변경",
            description = "이메일과 인증 코드를 검증하고, 성공 시 로그인한 단체의 이메일을 즉시 변경한다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "이메일 변경 및 세션 만료 성공",
            headers = @Header(
                    name = HttpHeaders.SET_COOKIE,
                    description = "관리자 Refresh Token 쿠키 삭제",
                    schema = @Schema(type = "string", example = "refreshToken=; Max-Age=0; Path=/")
            )
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.ALREADY_EXIST_EXCEPTION,
            ErrorCode.EMAIL_VERIFICATION_NOT_FOUND,
            ErrorCode.EMAIL_VERIFICATION_EXPIRED,
            ErrorCode.EMAIL_ALREADY_VERIFIED,
            ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH,
            ErrorCode.EMAIL_SAME_AS_CURRENT,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
    })
    public ResponseEntity<Void> verifyAdminEmail(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminEmailVerificationRequest request
    ) {
        emailVerificationService.verifyChangeEmail(request, authUser.organizationId());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.delete().toString())
                .build();
    }
}
