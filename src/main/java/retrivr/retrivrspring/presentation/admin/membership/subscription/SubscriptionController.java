package retrivr.retrivrspring.presentation.admin.membership.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionService;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionPlanChangeRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionCancelResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionPlanChangeResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionPaymentPreviewResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/subscriptions")
@Tag(name = "Admin API / Subscription API", description = "조직 구독 관리")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  @GetMapping("/preview")
  @Operation(
      summary = "구독 결제 정보 미리보기",
      description = """
          조직의 현재 이용권 상태와 선택한 플랜을 기준으로 즉시 결제 정보와 다음 결제 정보를 조회합니다.
          실제 구독 시작 시점에는 최신 상태를 기준으로 결제 정보가 다시 계산됩니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "구독 결제 정보 조회 성공",
      content = @Content(schema = @Schema(implementation = SubscriptionPaymentPreviewResponse.class))
  )
  public SubscriptionPaymentPreviewResponse getSubscriptionPaymentPreview(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @RequestParam SubscriptionPlan plan
  ) {
    return subscriptionService.getSubscriptionPaymentPreview(loginUser.organizationId(), plan);
  }

  @PostMapping
  @Operation(
      summary = "구독 시작",
      description = """
          조직의 월간/연간 구독을 시작합니다.
          결제 성공 후 Subscription을 활성화하고 MembershipPass를 생성합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "구독 시작 성공",
      content = @Content(schema = @Schema(implementation = SubscriptionStartResponse.class))
  )
  public SubscriptionStartResponse startSubscription(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Valid @RequestBody SubscriptionStartRequest request
  ) {
    return subscriptionService.startSubscription(loginUser.organizationId(), request);
  }

  @PatchMapping("/me/cancel")
  @Operation(
      summary = "구독 해지",
      description = """
          현재 조직의 구독을 해지합니다.
          구독 해지는 자동 결제만 중단하며 현재 사용 중인 MembershipPass는 만료 시점까지 유지합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "구독 해지 성공",
      content = @Content(schema = @Schema(implementation = SubscriptionCancelResponse.class))
  )
  public SubscriptionCancelResponse cancelSubscription(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return subscriptionService.cancelSubscription(loginUser.organizationId());
  }

  @PatchMapping("/plans")
  @Operation(
      summary = "구독 플랜 변경",
      description = """
          현재 조직의 구독 플랜을 변경합니다.
          기존 예약 결제를 취소하고, 다음 결제일에 변경된 플랜 가격으로 예약 결제를 다시 등록합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "구독 플랜 변경 성공",
      content = @Content(schema = @Schema(implementation = SubscriptionPlanChangeResponse.class))
  )
  public SubscriptionPlanChangeResponse changeSubscriptionPlan(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Valid @RequestBody SubscriptionPlanChangeRequest request
  ) {
    return subscriptionService.changeSubscriptionPlan(loginUser.organizationId(), request);
  }
}
