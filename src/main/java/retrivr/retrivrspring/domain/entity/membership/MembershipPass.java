package retrivr.retrivrspring.domain.entity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipLevel;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassType;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "membership_pass"
)
public class MembershipPass extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  private MembershipLevel level; // PREMIUM

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MembershipPassStatus status; // REGISTERED, ACTIVE, EXPIRED

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MembershipPassType sourceType; // COUPON, SUBSCRIPTION

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id")
  private Subscription subscription; // source Subscription

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_registration_id")
  private CouponRegistration couponRegistration; // source Coupon

  @Column(nullable = false)
  private LocalDateTime startAt; // 시작 시간

  @Column(nullable = false)
  private LocalDateTime endAt; // 종료 시간

  @Column(nullable = false)
  private Long sequence; // 순서

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  public static MembershipPass createCouponPass(
      Organization organization,
      MembershipLevel level,
      CouponRegistration couponRegistration,
      LocalDateTime startAt,
      int durationDays,
      Long sequence
  ) {
    Objects.requireNonNull(couponRegistration, "couponRegistration must not be null");
    if (durationDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION,
          "durationDays must be greater than 0");
    }
    return MembershipPass.builder()
        .organization(organization)
        .status(MembershipPassStatus.REGISTERED)
        .level(level)
        .sourceType(MembershipPassType.COUPON)
        .couponRegistration(couponRegistration)
        .startAt(startAt)
        .endAt(startAt.plusDays(durationDays))
        .sequence(sequence)
        .build();
  }

  public static MembershipPass createSubscriptionPass(
      Organization organization,
      MembershipLevel level,
      Subscription subscription,
      LocalDateTime startAt,
      int durationDays,
      Long sequence,
      Payment payment
  ) {
    Objects.requireNonNull(subscription, "subscription must not be null");
    if (durationDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION, "durationDays must be greater than 0");
    }
    return MembershipPass.builder()
        .organization(organization)
        .level(level)
        .status(MembershipPassStatus.REGISTERED)
        .sourceType(MembershipPassType.SUBSCRIPTION)
        .subscription(subscription)
        .startAt(startAt)
        .endAt(startAt.plusDays(durationDays))
        .sequence(sequence)
        .payment(payment)
        .build();
  }

  public void activate(LocalDateTime now) {
    if (!isActivable(now)) {
      throw new DomainException(ErrorCode.DO_NOT_ACTIVE_MEMBERSHIP_PASS);
    }
    this.status = MembershipPassStatus.ACTIVE;
  }

  public boolean isActivable(LocalDateTime now) {
    return this.status == MembershipPassStatus.REGISTERED && !this.startAt.isAfter(now);
  }

  public void expire(LocalDateTime now) {
    if (!isActive()) {
      throw new DomainException(ErrorCode.DO_NOT_EXPIRE_MEMBERSHIP_PASS);
    }
    this.status = MembershipPassStatus.EXPIRED;
  }

  public boolean isActive() {
    return this.status == MembershipPassStatus.ACTIVE;
  }

  // endAt 이 아닌 status 값을 기준으로 검사한다.
  // 스케쥴러가 status 를 변경하고 있으며, 기획에 따라 스케쥴러가 돌기 전까지 활성화 된 것으로 판단한다.
  // 스케쥴러가 돌았을 때 자동결제가 되므로 중간에 프리미엄이 꺼지지 않도록 하기 위함이다.
  public boolean isExpired(LocalDateTime now) {
    return this.status == MembershipPassStatus.EXPIRED;
  }

  public boolean isOverDue(LocalDateTime now) {
    return this.endAt.isBefore(now);
  }

  public boolean isSubscriptionPass() {
    return this.sourceType == MembershipPassType.SUBSCRIPTION;
  }

  public CouponRegistration getCouponRegistrationOrThrow() {
    if (this.couponRegistration == null) {
      throw new DomainException(ErrorCode.DO_NOT_GET_COUPON_REGISTRATION);
    }
    return this.couponRegistration;
  }

  public Payment getPaymentOrThrow() {
    if (this.payment == null) {
      throw new DomainException(ErrorCode.DO_NOT_GET_PAYMENT);
    }
    return this.payment;
  }

  public Subscription getSubscriptionOrThrow() {
    if (this.subscription == null) {
      throw new DomainException(ErrorCode.DO_NOT_GET_SUBSCRIPTION);
    }
    return this.subscription;
  }
}
