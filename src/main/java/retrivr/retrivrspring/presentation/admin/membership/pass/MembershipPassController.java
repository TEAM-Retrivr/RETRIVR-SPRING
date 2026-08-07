package retrivr.retrivrspring.presentation.admin.membership.pass;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.MembershipStatusSummaryResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.CurrentSubscriptionMembershipPassResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.CouponMembershipPassListResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.MembershipPassHistoryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/memberships")
@Tag(name = "Admin API / Membership API", description = "조직의 멤버십 상태 조회")
public class MembershipPassController {

  private final MembershipPassService membershipPassService;

  @GetMapping("/current")
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

  @GetMapping("/current/subscription")
  @Operation(
      summary = "현재 이용 중인 구독 이용권 조회",
      description = "현재 활성화된 이용권이 구독으로 발급된 경우 구독 이용권 정보를 반환합니다."
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.NOT_FOUND_ACTIVE_PASS
  })
  public ResponseEntity<CurrentSubscriptionMembershipPassResponse> getCurrentSubscriptionMembershipPass(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    CurrentSubscriptionMembershipPassResponse response = membershipPassService.getCurrentSubscriptionMembershipPass(
        loginUser.organizationId());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/coupons")
  @Operation(
      summary = "쿠폰 이용권 조회",
      description = "현재 사용 중이거나 사용 대기 중인 쿠폰 이용권 목록을 반환합니다."
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION
  })
  public ResponseEntity<CouponMembershipPassListResponse> getCouponMembershipPasses(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    CouponMembershipPassListResponse response = membershipPassService.getCouponMembershipPasses(loginUser.organizationId());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/history")
  @Operation(
      summary = "이용권 및 결제 내역 조회",
      description = "조직의 전체 이용권 내역과 구독 이용권에 연결된 결제 정보를 반환합니다."
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION
  })
  public ResponseEntity<MembershipPassHistoryResponse> getMembershipHistory(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Parameter(description = "커서(마지막으로 조회한 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size,
      @RequestParam(name = "start", required = false) LocalDate start,
      @RequestParam(name = "end", required = false) LocalDate end
  ) {
    MembershipPassHistoryResponse response = membershipPassService.getMembershipPassHistory(loginUser.organizationId(), cursor, size, start, end);
    return ResponseEntity.ok(response);
  }
}
