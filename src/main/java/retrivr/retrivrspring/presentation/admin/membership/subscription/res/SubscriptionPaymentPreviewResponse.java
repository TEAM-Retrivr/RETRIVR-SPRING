package retrivr.retrivrspring.presentation.admin.membership.subscription.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record SubscriptionPaymentPreviewResponse(
    @Schema(description = "결제 시나리오")
    Scenario scenario,
    @Schema(description = "결제에 적용되는 구독 플랜")
    SubscriptionPlan plan,
    @Schema(description = "구독 플랜 결제 금액", example = "4900")
    int amount,
    @Schema(description = "구독 시작 요청 시 즉시 결제되는 금액. 예약 결제 또는 기존 구독이면 null")
    Integer immediatePaymentAmount,
    @Schema(description = "즉시 결제 예정 시각. 예약 결제 또는 기존 구독이면 null")
    LocalDateTime immediatePaymentAt,
    @Schema(description = "다음 결제 예정 금액")
    int nextBillingAmount,
    @Schema(description = "다음 결제 예정 시각")
    LocalDateTime nextBillingAt,
    @Schema(description = "선택한 구독 이용권이 적용되는 시각")
    LocalDateTime effectiveAt
) {

  public enum Scenario {
    IMMEDIATE_PURCHASE,
    DEFERRED_START
  }

  public static SubscriptionPaymentPreviewResponse immediate(
      SubscriptionPlan plan,
      LocalDateTime now
  ) {
    LocalDateTime nextBillingAt = now.plusDays(plan.getDuration());
    return new SubscriptionPaymentPreviewResponse(
        Scenario.IMMEDIATE_PURCHASE,
        plan,
        plan.getPrice(),
        plan.getPrice(),
        now,
        plan.getPrice(),
        nextBillingAt,
        now
    );
  }

  public static SubscriptionPaymentPreviewResponse deferred(
      SubscriptionPlan plan,
      LocalDateTime effectiveAt
  ) {
    return new SubscriptionPaymentPreviewResponse(
        Scenario.DEFERRED_START,
        plan,
        plan.getPrice(),
        null,
        null,
        plan.getPrice(),
        effectiveAt,
        effectiveAt
    );
  }

}
