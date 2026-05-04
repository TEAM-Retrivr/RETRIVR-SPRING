package retrivr.retrivrspring.presentation.open.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.open.PublicPhoneVerificationService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.open.auth.req.PhoneVerificationSendRequest;
import retrivr.retrivrspring.presentation.open.auth.req.PhoneVerificationVerifyRequest;
import retrivr.retrivrspring.presentation.open.auth.res.PhoneVerificationSendResponse;
import retrivr.retrivrspring.presentation.open.auth.res.PhoneVerificationVerifyResponse;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public API / Phone Verification", description = "대여자용 핸드폰 번호 인증")
@RequestMapping("/api/public/v1/auth/phone-verification")
public class PublicPhoneVerificationController {

  private final PublicPhoneVerificationService publicPhoneVerificationService;

  @PostMapping("/send-code")
  @Operation(summary = "핸드폰 번호 인증번호 발송")
  @ApiResponse(
      responseCode = "200",
      description = "인증번호 발송 성공",
      content = @Content(schema = @Schema(implementation = PhoneVerificationSendResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.TOO_MANY_PHONE_VERIFICATION_REQUEST
  })
  public PhoneVerificationSendResponse sendVerificationCode(
      @RequestBody @Valid PhoneVerificationSendRequest request
  ) {
    return publicPhoneVerificationService.sendVerificationCode(request);
  }

  @PostMapping("/verify-code")
  @Operation(summary = "핸드폰 번호 인증번호 검증")
  @ApiResponse(
      responseCode = "200",
      description = "인증번호 검증 성공",
      content = @Content(schema = @Schema(implementation = PhoneVerificationVerifyResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_PHONE_VERIFICATION,
      ErrorCode.PHONE_VERIFICATION_PURPOSE_MISMATCH,
      ErrorCode.EXPIRED_PHONE_VERIFICATION,
      ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH,
      ErrorCode.TOO_MANY_PHONE_VERIFICATION_ATTEMPTS
  })
  public PhoneVerificationVerifyResponse verifyCode(
      @RequestBody @Valid PhoneVerificationVerifyRequest request
  ) {
    return publicPhoneVerificationService.verifyCode(request);
  }
}
