package retrivr.retrivrspring.presentation.admin.membership.pass.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

@Schema(description = "현재 구독 이용권 응답")
public record CurrentSubscriptionMembershipPassResponse(
    @Schema(description = "이용권 ID")
    String membershipPassId,

    @Schema(description = "구독 플랜", example = "YEARLY")
    SubscriptionPlan plan,

    @Schema(description = "이용권 상태", example = "ACTIVE")
    MembershipPassStatus membershipPassStatus,

    @Schema(description = "이용권 기간(일)", example = "365")
    int durationDays,

    @Schema(description = "최근 결제 금액", example = "46900")
    long paidAmount,

    @Schema(description = "이용권 시작 일시")
    LocalDateTime startAt,

    @Schema(description = "이용권 종료 일시")
    LocalDateTime endAt,

    @Schema(description = "다음 자동 결제 예정 일시. 구독 해지 또는 결제 실패 상태이면 null")
    LocalDateTime nextBillingAt
) {

  public static CurrentSubscriptionMembershipPassResponse of(
      Subscription subscription,
      MembershipPass membershipPass
  ) {
    Payment payment = membershipPass.getPaymentOrThrow();

    return new CurrentSubscriptionMembershipPassResponse(
        membershipPass.getId(),
        payment.getPlan(),
        membershipPass.getStatus(),
        payment.getPlan().getDuration(),
        payment.getAmount(),
        membershipPass.getStartAt(),
        membershipPass.getEndAt(),
        subscription.getNextBillingAt()
    );
  }
}
