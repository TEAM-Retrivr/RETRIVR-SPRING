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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "payment"
)
public class Payment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionPlan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status; // SUCCESS, FAILED, SCHEDULED, SCHEDULE_CANCELED

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider; // MOCK, TOSS

  @Column(nullable = false)
  private Long amount;

  @Column
  private String providerPaymentKey;

  @Column
  private String failureCode;

  @Column
  private String failureReason;

  @Column
  private LocalDateTime paidAt;

  @Column
  private LocalDateTime failedAt;

  @Column
  private LocalDateTime scheduledAt;

  @Column
  private LocalDateTime canceledAt;

  public static Payment success(
      String paymentId,
      SubscriptionPlan plan,
      Organization organization,
      Long amount,
      PaymentProvider provider,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    return Payment.builder()
        .id(paymentId)
        .organization(organization)
        .plan(plan)
        .status(PaymentStatus.SUCCESS)
        .provider(provider)
        .amount(amount)
        .providerPaymentKey(
            providerPaymentKey == null || providerPaymentKey.isBlank()
                ? provider.name().toLowerCase() + "_" + UUID.randomUUID()
                : providerPaymentKey
        )
        .paidAt(paidAt)
        .build();
  }

  public static Payment fail(
      String paymentId,
      SubscriptionPlan plan,
      Organization organization,
      Long amount,
      PaymentProvider provider,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    return Payment.builder()
        .id(paymentId)
        .organization(organization)
        .plan(plan)
        .status(PaymentStatus.FAILED)
        .provider(provider)
        .amount(amount)
        .failureCode(failureCode)
        .failureReason(failureReason)
        .failedAt(failedAt)
        .build();
  }

  public static Payment schedule(
      String paymentId,
      SubscriptionPlan plan,
      Organization organization,
      Long amount,
      PaymentProvider provider,
      LocalDateTime scheduledAt
  ) {
    return Payment.builder()
        .id(paymentId)
        .organization(organization)
        .plan(plan)
        .status(PaymentStatus.SCHEDULED)
        .provider(provider)
        .amount(amount)
        .scheduledAt(scheduledAt)
        .build();
  }

  public void scheduledCancel(LocalDateTime canceledAt) {
    if (!isScheduled()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.SCHEDULE_CANCELED;
    this.canceledAt = canceledAt;
  }

  public void scheduledPaymentSuccess(
      PaymentProvider provider,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    if (!isScheduled()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.SUCCESS;
    this.provider = provider;
    this.providerPaymentKey = providerPaymentKey;
    this.paidAt = paidAt;
  }

  public void scheduledPaymentFail(
      PaymentProvider provider,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    if (!isScheduled()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.FAILED;
    this.provider = provider;
    this.failureCode = failureCode;
    this.failureReason = failureReason;
    this.failedAt = failedAt;
  }

  public boolean isSuccess() {
    return this.status == PaymentStatus.SUCCESS;
  }

  public boolean isFailed() {
    return this.status == PaymentStatus.FAILED;
  }

  public boolean isScheduled() { return this.status == PaymentStatus.SCHEDULED; }

  public boolean isCanceled() {return this.status == PaymentStatus.SCHEDULE_CANCELED; }

  public void validateOwner(Organization owner) {
    if (!Objects.equals(owner.getId(), organization.getId())) {
      throw new DomainException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);//PAYMENT_OWNER_MISMATCH);
    }
  }
}
