package retrivr.retrivrspring.presentation.admin.membership.pass.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassType;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

@Schema(description = "이용권 사용 및 결제 내역 응답")
public record MembershipPassHistoryResponse(
    List<MembershipPassHistoryItemResponse> items,
    Long nextCursor
) {

  public static MembershipPassHistoryResponse from(List<MembershipPass> membershipPasses, Long nextCursor) {
    return new MembershipPassHistoryResponse(
        membershipPasses.stream()
            .map(MembershipPassHistoryItemResponse::from)
            .toList(),
        nextCursor
    );
  }

  @Schema(description = "이용권 사용 또는 결제 내역")
  public record MembershipPassHistoryItemResponse(
      @Schema(description = "이용권 ID")
      String membershipPassId,

      @Schema(description = "내역 유형", example = "SUBSCRIPTION")
      MembershipPassType type,

      @Schema(description = "쿠폰명 혹은 구독플랜 이름")
      String title,

      @Schema(description = "이용권 상태", example = "EXPIRED")
      MembershipPassStatus status,

      @Schema(description = "결제 플랜. 쿠폰 내역이면 null", example = "MONTHLY")
      SubscriptionPlan plan,

      @Schema(description = "결제 또는 쿠폰 사용 일시", example = "2026.08.08(토) 13:11")
      String occurredAt,

      @Schema(description = "결제 금액. 쿠폰 내역이면 0", example = "4900")
      long amount,

      @Schema(description = "영수증 조회 가능 여부")
      boolean receiptAvailable
  ) {

    public static MembershipPassHistoryItemResponse from(MembershipPass membershipPass) {
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("yyyy.MM.dd(E) HH:mm", Locale.KOREA);

      if (membershipPass.isSubscriptionPass()) {
        Payment payment = membershipPass.getPaymentOrThrow();
        return new MembershipPassHistoryItemResponse(
            membershipPass.getId(),
            membershipPass.getSourceType(),
            payment.getPlan().getKorean() + " 이용권 결제",
            membershipPass.getStatus(),
            payment.getPlan(),
            payment.getPaidAt().format(formatter),
            payment.getAmount(),
            payment.isSuccess()
        );
      }

      Coupon coupon = membershipPass.getCouponRegistrationOrThrow().getCoupon();
      return new MembershipPassHistoryItemResponse(
          membershipPass.getId(),
          membershipPass.getSourceType(),
          coupon.getName(),
          membershipPass.getStatus(),
          null,
          membershipPass.getCreatedAt().format(formatter),
          0L,
          false
      );
    }
  }
}
