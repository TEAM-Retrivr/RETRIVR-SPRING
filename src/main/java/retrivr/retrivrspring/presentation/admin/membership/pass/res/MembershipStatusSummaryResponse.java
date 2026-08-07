package retrivr.retrivrspring.presentation.admin.membership.pass.res;

import java.time.LocalDate;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipLevel;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;

public record MembershipStatusSummaryResponse(
    boolean subscribed,
    MembershipLevel level,
    String passType,
    CouponInfo couponInfo,
    SubscriptionInfo subscriptionInfo,
    LocalDate startAt,
    LocalDate endAt,
    LocalDate nextBillingAt,
    Long payedAmount
) {

  public record CouponInfo(
      String couponName,
      String couponDescription
  ) {

  }

  public record SubscriptionInfo(
      String subscriptionName
  ) {

  }

  public static MembershipStatusSummaryResponse freePlan() {
    return new MembershipStatusSummaryResponse(
        false,
        MembershipLevel.FREE,
        null,
        null,
        null,
        null,
        null,
        null,
        0L
    );
  }

  public static MembershipStatusSummaryResponse subscribedPlan(
      MembershipPass membershipPass
  ) {
    Payment payment = membershipPass.getPaymentOrThrow();
    Subscription subscription = membershipPass.getSubscriptionOrThrow();

    String subscriptionName = "";
    String passType = "";
    if (payment.getPlan() == SubscriptionPlan.MONTHLY) {
      subscriptionName = "월간 이용권";
      passType = "월간 구독";
    }
    else {
      subscriptionName = "연간 이용권";
      passType = "연간 구독";
    }

    LocalDate nextBillingAt = subscription.getNextBillingAt() != null ? LocalDate.from(subscription.getNextBillingAt()) : null;

    return new MembershipStatusSummaryResponse(
        subscription.isActive(),
        membershipPass.getLevel(),
        passType,
        null,
        new SubscriptionInfo(subscriptionName),
        LocalDate.from(membershipPass.getStartAt()),
        LocalDate.from(membershipPass.getEndAt()),
        nextBillingAt,
        payment.getAmount()
    );
  }

  public static MembershipStatusSummaryResponse couponPlanWithSubscription(
      MembershipPass membershipPass,
      Subscription subscription
  ) {
    Coupon coupon = membershipPass.getCouponRegistrationOrThrow()
        .getCoupon();

    LocalDate nextBillingAt = subscription.getNextBillingAt() != null ? LocalDate.from(subscription.getNextBillingAt()) : null;

    return new MembershipStatusSummaryResponse(
        subscription.isActive(),
        membershipPass.getLevel(),
        "쿠폰 사용",
        new CouponInfo(coupon.getName(), coupon.getDescription()),
        null,
        LocalDate.from(membershipPass.getStartAt()),
        LocalDate.from(membershipPass.getEndAt()),
        nextBillingAt,
        0L
    );
  }

  public static MembershipStatusSummaryResponse couponPlanWithoutSubscription(
      MembershipPass membershipPass
  ) {
    Coupon coupon = membershipPass.getCouponRegistration().getCoupon();
    return new MembershipStatusSummaryResponse(
        false,
        membershipPass.getLevel(),
        "쿠폰 사용",
        new CouponInfo(coupon.getName(), coupon.getDescription()),
        null,
        LocalDate.from(membershipPass.getStartAt()),
        LocalDate.from(membershipPass.getEndAt()),
        null,
        0L
    );
  }
}
