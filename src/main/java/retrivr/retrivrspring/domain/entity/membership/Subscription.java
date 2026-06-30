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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "subscription",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_org",
            columnNames = {"organization_id"}
        )
    }
)
public class Subscription extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  private SubscriptionPlan plan; // MONTHLY, YEARLY

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status; // ACTIVE, CANCELED, PAST_DUE, PAYMENT_FAILED

  private LocalDateTime nextBillingAt; // 다음 결제 시각

  private LocalDateTime startedAt; // 최초 구독 시작 시각

  private LocalDateTime canceledAt; // 해지 시각

  private LocalDateTime paymentFailedAt; // 마지막 결제 실패 시각

  private long paymentFailCount;

  private static final int MAX_PAYMENT_FAIL_COUNT = 3;

  public void updateNextBillingAt(LocalDateTime paidAt) {
    if (!isActive()) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_STATUS_CONFLICT);
    }

    if (this.plan == SubscriptionPlan.MONTHLY) {
      this.nextBillingAt = paidAt.plusMonths(1);
      return;
    }

    if (this.plan == SubscriptionPlan.YEARLY) {
      this.nextBillingAt = paidAt.plusYears(1);
    }
  }

  public void completeSuccessfulPayment() {
    this.status = SubscriptionStatus.ACTIVE;
    this.paymentFailedAt = null;
    this.paymentFailCount = 0;
  }

  public void scheduleNextBillingAt(LocalDateTime nextBillingAt) {
    if (!isActive()) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_STATUS_CONFLICT);
    }
    this.nextBillingAt = nextBillingAt;
  }

  private void retryableFailedPayment(LocalDateTime failedAt) {
    this.status = SubscriptionStatus.PAST_DUE;
    this.paymentFailedAt = failedAt;
  }

  private void finalFailedPayment(LocalDateTime failedAt) {
    this.status = SubscriptionStatus.PAYMENT_FAILED;
    this.paymentFailedAt = failedAt;
    this.nextBillingAt = null;
  }

  public void failPayment(LocalDateTime failedAt) {
    this.paymentFailCount++;
    if (this.paymentFailCount >= MAX_PAYMENT_FAIL_COUNT) {
      finalFailedPayment(failedAt);
    } else {
      retryableFailedPayment(failedAt);
    }
  }

  public boolean isPastDue() {
    return this.status == SubscriptionStatus.PAST_DUE;
  }

  public boolean isPaymentFailed() {
    return this.status == SubscriptionStatus.PAYMENT_FAILED;
  }

  public static Subscription start(Organization organization, SubscriptionPlan plan,
      LocalDateTime now) {
    switch (plan) {
      case MONTHLY:
        return createMonthly(organization, now);
      case YEARLY:
        return createYearly(organization, now);
      default:
        throw new DomainException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }
  }

  private static Subscription createMonthly(Organization organization, LocalDateTime now) {
    return Subscription.builder()
        .organization(organization)
        .plan(SubscriptionPlan.MONTHLY)
        .status(SubscriptionStatus.ACTIVE)
        .nextBillingAt(now.plusMonths(1))
        .startedAt(now)
        .build();
  }

  private static Subscription createYearly(Organization organization, LocalDateTime now) {
    return Subscription.builder()
        .organization(organization)
        .plan(SubscriptionPlan.YEARLY)
        .status(SubscriptionStatus.ACTIVE)
        .nextBillingAt(now.plusYears(1))
        .startedAt(now)
        .build();
  }

  public Subscription restart(Organization organization, SubscriptionPlan plan, LocalDateTime now, @Nullable LocalDateTime passEndDate) {
    validateOwner(organization);
    if (isActive()) {
      throw new DomainException(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    }
    switch (plan) {
      case MONTHLY:
        this.nextBillingAt = now.plusMonths(1);
        break;
      case YEARLY:
        this.nextBillingAt = now.plusYears(1);
        break;
      default:
        throw new DomainException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }

    if (passEndDate != null) {
      this.nextBillingAt = passEndDate;
    }

    this.status = SubscriptionStatus.ACTIVE;
    this.plan = plan;
    this.paymentFailedAt = null;
    this.canceledAt = null;
    this.paymentFailCount = 0;

    return this;
  }

  public void cancel(Organization organization, LocalDateTime now) {
    validateOwner(organization);
    if (!isActive()) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_STATUS_CONFLICT, "이미 구독 되지 않은 상태입니다.");
    }
    this.status = SubscriptionStatus.CANCELED;
    this.canceledAt = now;
    this.plan = null;
    this.paymentFailedAt = null;
    this.nextBillingAt = null;
  }

  public void validateOwner(Organization owner) {
    if (!Objects.equals(owner.getId(), organization.getId())) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_OWNER_MISMATCH);
    }
  }

  public int getDurationDays() {
    if (this.plan == SubscriptionPlan.MONTHLY) {
      return 30;
    }
    if (this.plan == SubscriptionPlan.YEARLY) {
      return 365;
    }
    throw new DomainException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
  }

  public boolean isActive() {
    return this.status == SubscriptionStatus.ACTIVE || this.status == SubscriptionStatus.PAST_DUE;
  }

  public LocalDateTime getNextBillingAt() {
    if (!isActive()) {
      return null;
    }
    return this.nextBillingAt;
  }

  public boolean isPaused() {
    return this.status == SubscriptionStatus.CANCELED || this.status == SubscriptionStatus.PAYMENT_FAILED;
  }
}
