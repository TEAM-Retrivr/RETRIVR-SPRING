package retrivr.retrivrspring.presentation.admin.membership;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.membership.res.MembershipStatusSummaryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/membership")
@Tag(name = "Admin API / Membership API", description = "조직의 멤버십 상태 조회")
public class MembershipPassController {

  private final MembershipPassService membershipPassService;

  @GetMapping
  @Operation(
      summary = "현재 멤버십 상태 조회",
      description = """
          현재 조직의 멤버십 상태를 조회합니다.
          구독 여부, Pro 이용 가능 여부, 현재 이용권, 다음 이용권 정보를 반환합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "멤버십 상태 조회 성공",
      content = @Content(schema = @Schema(implementation = MembershipStatusSummaryResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.DO_NOT_GET_SUBSCRIPTION,
      ErrorCode.DO_NOT_GET_COUPON_REGISTRATION
  })
  public MembershipStatusSummaryResponse getMembership(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return membershipPassService.getMembershipStatusSummary(loginUser.organizationId());
  }
}
