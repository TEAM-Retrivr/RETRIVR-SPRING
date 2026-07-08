package retrivr.retrivrspring.presentation.admin.membership.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionCancelResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/subscriptions")
@Tag(name = "Admin API / Subscription API", description = "조직 구독 관리")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

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
}
