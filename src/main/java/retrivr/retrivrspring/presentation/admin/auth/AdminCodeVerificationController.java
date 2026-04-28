package retrivr.retrivrspring.presentation.admin.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import retrivr.retrivrspring.application.service.admin.auth.AdminCodeVerificationService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminCodeVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminCodeVerificationRequestForPublic;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminCodeVerificationResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminCodeVerificationResponseForPublic;

@RestController
@RequestMapping("/api")
@Tag(name = "Admin API / Admin Code Verification", description = "관리자 코드 인증 관련 API")
@RequiredArgsConstructor
public class AdminCodeVerificationController {

  private final AdminCodeVerificationService adminCodeVerificationService;

  @PostMapping("/admin/v1/admin-code/verification")
  @Operation(
      summary = "관리자 코드 검증",
      description = "입력한 관리자 코드를 검증하고, 성공하면 관리자 코드 검증 토큰을 발급합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "검증 성공",
      content = @Content(schema = @Schema(implementation = AdminCodeVerificationResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.ADMIN_CODE_MISMATCH
  })
  public AdminCodeVerificationResponse verifyAdminCode(
      @Valid @RequestBody AdminCodeVerificationRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminCodeVerificationService.verifyAdminCode(loginUser.organizationId(), request);
  }

  @PostMapping("/public/v1/admin-code/verification")
  @Operation(
      summary = "관리자 코드 검증",
      description = "입력한 관리자 코드를 검증하고, 성공하면 관리자 코드 검증 토큰을 발급합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "검증 성공",
      content = @Content(schema = @Schema(implementation = AdminCodeVerificationResponseForPublic.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.ADMIN_CODE_MISMATCH
  })
  public AdminCodeVerificationResponseForPublic verifyAdminCodeForPublic(
      @Valid @RequestBody AdminCodeVerificationRequestForPublic request
  ) {
    return adminCodeVerificationService.verifyAdminCode(request);
  }
}
