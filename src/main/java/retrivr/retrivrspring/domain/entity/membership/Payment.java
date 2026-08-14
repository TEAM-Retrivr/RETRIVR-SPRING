package retrivr.retrivrspring.domain.entity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;
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
public class Payment extends BaseTimeEntity implements Persistable<String> {

  @Id
  private String id;

  @Override
  public String getId() {
    return id;
  }

  @Override
  @Transient
  public boolean isNew() {
    return getCreatedAt() == null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionPlan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider; // MOCK, TOSS

  @Column
  private String portOneScheduleId;

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

  public static Payment pending(
      String paymentId,
      SubscriptionPlan plan,
      Organization organization,
      Long amount,
      PaymentProvider provider
  ) {
    return Payment.builder()
        .id(paymentId)
        .organization(organization)
        .plan(plan)
        .status(PaymentStatus.PENDING)
        .provider(provider)
        .amount(amount)
        .build();
  }

  public void completeImmediatePayment(
      String portOneScheduleId,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    if (!isPending()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.SUCCESS;
    this.portOneScheduleId = portOneScheduleId;
    this.providerPaymentKey = providerPaymentKey;
    this.paidAt = paidAt;
  }

  public void failPendingPayment(
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    if (!isPending()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.FAILED;
    this.failureCode = failureCode;
    this.failureReason = failureReason;
    this.failedAt = failedAt;
  }

  public void markPaymentUnknown(
      String reason,
      LocalDateTime occurredAt
  ) {
    if (!isPending()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.UNKNOWN;
    this.failureReason = reason;
    this.failedAt = occurredAt;
  }

  public void requireCompensation(String reason) {
    if (!isSuccess()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.COMPENSATION_REQUIRED;
    this.failureCode = "SUBSCRIPTION_ACTIVATION_FAILED";
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = LocalDateTime.now();
  }

  public void startRefund(LocalDateTime requestedAt) {
    if (!isCompensationRequired() && !isRefundUnknown() && !isRefundProcessing()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.REFUND_PROCESSING;
    this.failedAt = requestedAt;
  }

  public void completeRefund(LocalDateTime refundedAt) {
    if (!isRefundProcessing() && !isRefundUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.REFUNDED;
    this.canceledAt = refundedAt;
    this.failureCode = null;
    this.failureReason = null;
  }

  public void markRefundUnknown(String reason, LocalDateTime checkedAt) {
    if (!isRefundProcessing() && !isRefundUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.REFUND_UNKNOWN;
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = checkedAt;
  }

  public void failRefund(String reason, LocalDateTime failedAt) {
    if (!isRefundProcessing() && !isRefundUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.REFUND_FAILED;
    this.failureCode = "PORTONE_REFUND_FAILED";
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = failedAt;
  }

  public void resolveUnknownAsSuccess(
      String portOneScheduleId,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    if (!isUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.SUCCESS;
    this.portOneScheduleId = portOneScheduleId;
    this.providerPaymentKey = providerPaymentKey;
    this.paidAt = paidAt;
    this.failureCode = null;
    this.failureReason = null;
    this.failedAt = null;
  }

  public void resolveUnknownAsFailed(
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    if (!isUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.FAILED;
    this.failureCode = failureCode;
    this.failureReason = failureReason;
    this.failedAt = failedAt;
  }

  public void deferUnknownReconciliation(
      String reason,
      LocalDateTime retriedAt
  ) {
    if (!isUnknown()) {
      return;
    }
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = retriedAt;
  }

  public static Payment success(
      String paymentId,
      SubscriptionPlan plan,
      Organization organization,
      String portOneScheduleId,
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
        .portOneScheduleId(portOneScheduleId)
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
      String portOneScheduleId,
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
        .portOneScheduleId(portOneScheduleId)
        .amount(amount)
        .scheduledAt(scheduledAt)
        .build();
  }

  public static Payment pendingSchedule(
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
        .status(PaymentStatus.SCHEDULE_PENDING)
        .provider(provider)
        .amount(amount)
        .failedAt(LocalDateTime.now())
        .scheduledAt(scheduledAt)
        .build();
  }

  public void completeSchedule(String scheduleId) {
    if (!isSchedulePending() && !isScheduleUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.SCHEDULED;
    this.portOneScheduleId = scheduleId;
    this.failureReason = null;
    this.failedAt = null;
  }

  public void markScheduleUnknown(String reason, LocalDateTime checkedAt) {
    if (!isSchedulePending() && !isScheduleUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    this.status = PaymentStatus.SCHEDULE_UNKNOWN;
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = checkedAt;
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
    if (!isScheduled() && !isScheduleCancelPending() && !isScheduleCancelUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.FAILED;
    this.provider = provider;
    this.failureCode = failureCode;
    this.failureReason = failureReason;
    this.failedAt = failedAt;
  }

  public void requireCompensationForScheduledPayment(
      PaymentProvider provider,
      String providerPaymentKey,
      LocalDateTime paidAt,
      LocalDateTime detectedAt
  ) {
    if (!isScheduleCancelPending() && !isScheduleCancelUnknown()) {
      throw new DomainException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    this.status = PaymentStatus.COMPENSATION_REQUIRED;
    this.provider = provider;
    this.providerPaymentKey = providerPaymentKey;
    this.paidAt = paidAt;
    this.failureCode = "PAYMENT_COMPLETED_DURING_SCHEDULE_CANCELLATION";
    this.failureReason = "예약 취소 처리 중 결제가 완료되어 환불이 필요합니다.";
    this.failedAt = detectedAt;
  }

  public void requestScheduleCancellation(LocalDateTime requestedAt) {
    if (!isScheduled()) {
      throw new DomainException(
          ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION
      );
    }

    this.status = PaymentStatus.SCHEDULE_CANCEL_PENDING;
    this.failureCode = null;
    this.failureReason = null;
    this.failedAt = requestedAt;
  }

  public void completeScheduleCancellation(LocalDateTime canceledAt) {
    if (!isScheduleCancelPending() && !isScheduleCancelUnknown()) {
      throw new DomainException(
          ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION
      );
    }

    this.status = PaymentStatus.SCHEDULE_CANCELED;
    this.canceledAt = canceledAt;
    this.failureCode = null;
    this.failureReason = null;
    this.failedAt = null;
  }

  public void markScheduleCancellationUnknown(
      String reason,
      LocalDateTime checkedAt
  ) {
    if (!isScheduleCancelPending() && !isScheduleCancelUnknown()) {
      throw new DomainException(
          ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION
      );
    }

    this.status = PaymentStatus.SCHEDULE_CANCEL_UNKNOWN;
    this.failureReason = normalizeFailureReason(reason);
    this.failedAt = checkedAt;
  }

  public boolean isSuccess() {
    return this.status == PaymentStatus.SUCCESS;
  }

  public boolean isPending() {
    return this.status == PaymentStatus.PENDING;
  }

  public boolean isUnknown() {
    return this.status == PaymentStatus.UNKNOWN;
  }

  public boolean isFailed() {
    return this.status == PaymentStatus.FAILED;
  }

  public boolean isCompensationRequired() {
    return this.status == PaymentStatus.COMPENSATION_REQUIRED;
  }

  public boolean isRefundProcessing() {
    return this.status == PaymentStatus.REFUND_PROCESSING;
  }

  public boolean isRefundUnknown() {
    return this.status == PaymentStatus.REFUND_UNKNOWN;
  }

  public boolean isRefunded() {
    return this.status == PaymentStatus.REFUNDED;
  }

  public boolean isScheduled() { return this.status == PaymentStatus.SCHEDULED; }

  public boolean isSchedulePending() {
    return this.status == PaymentStatus.SCHEDULE_PENDING;
  }

  public boolean isScheduleUnknown() {
    return this.status == PaymentStatus.SCHEDULE_UNKNOWN;
  }

  public boolean isScheduleCancelPending() {
    return this.status == PaymentStatus.SCHEDULE_CANCEL_PENDING;
  }

  public boolean isScheduleCancelUnknown() {
    return this.status == PaymentStatus.SCHEDULE_CANCEL_UNKNOWN;
  }

  public boolean isScheduleCanceled() {
    return this.status == PaymentStatus.SCHEDULE_CANCELED;
  }

  public void validateOwner(Organization owner) {
    if (!Objects.equals(owner.getId(), organization.getId())) {
      throw new DomainException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);//PAYMENT_OWNER_MISMATCH);
    }
  }

  private String normalizeFailureReason(String reason) {
    String normalized = reason == null || reason.isBlank()
        ? "구독 활성화 처리에 실패했습니다."
        : reason;
    return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
  }
}
