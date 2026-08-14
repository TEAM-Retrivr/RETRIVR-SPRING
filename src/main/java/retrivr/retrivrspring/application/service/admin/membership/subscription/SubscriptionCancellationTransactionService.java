package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
public class SubscriptionCancellationTransactionService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassRepository membershipPassRepository;

  @Transactional
  public BillingScheduleCancellationPreparation prepare(Long organizationId, LocalDateTime now) {
    Organization organization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION));

    subscription.validateOwner(organization);

    LocalDateTime currentPassExpireAt = currentPassExpireAt(organization);

    // 이미 구독 취소 상태인 경우
    if (subscription.isCanceled()) {
      Payment existingCancellation = paymentRepository
          .findFirstByOrganizationAndStatusInOrderByCreatedAtDesc(
              organization,
              List.of(
                  PaymentStatus.SCHEDULE_CANCEL_PENDING,
                  PaymentStatus.SCHEDULE_CANCEL_UNKNOWN,
                  PaymentStatus.SCHEDULE_CANCELED
              )
          )
          .orElse(null);

      // 구독 취소 상태이고 예약 결제도 없는 경우
      if (existingCancellation == null || existingCancellation.isScheduleCanceled()) {
        return BillingScheduleCancellationPreparation.notRequired(
            existingCancellation != null ? existingCancellation.getId() : null,
            subscription.getId(),
            subscription.getStatus(),
            subscription.getCanceledAt(),
            currentPassExpireAt
        );
      }

      // 구독 취소 상태이지만 예약 결제가 남아있는 경우
      return BillingScheduleCancellationPreparation.cancellationRequired(
          subscription,
          existingCancellation,
          currentPassExpireAt
      );
    }

    // 구독 상태 변경
    subscription.cancel(organization, now);

    Payment pendingPayment = paymentRepository.findByOrganizationAndStatus(organization,
            PaymentStatus.SCHEDULED)
        .orElse(null);

    if (pendingPayment == null) {
      return BillingScheduleCancellationPreparation.notRequired(
          null,
          subscription.getId(),
          subscription.getStatus(),
          subscription.getCanceledAt(),
          currentPassExpireAt
      );
    }

    pendingPayment.requestScheduleCancellation(now);

    return BillingScheduleCancellationPreparation.cancellationRequired(
        subscription,
        pendingPayment,
        currentPassExpireAt
    );
  }

  @Transactional(readOnly = true)
  public BillingScheduleCancellationPreparation prepareRetry(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));

    if (!isCancellationInProgress(payment)) {
      return BillingScheduleCancellationPreparation.notRequired(
          payment.getId(),
          null,
          null,
          payment.getCanceledAt(),
          null
      );
    }

    return new BillingScheduleCancellationPreparation(
        payment.getId(),
        payment.getPortOneScheduleId(),
        payment.getAmount(),
        true,
        null,
        null,
        null,
        null
    );
  }

  @Transactional
  public void complete(String paymentId, LocalDateTime canceledAt) {
    Payment payment = getPaymentWithLock(paymentId);
    if (!isCancellationInProgress(payment)) {
      return;
    }
    payment.completeScheduleCancellation(canceledAt);
  }

  @Transactional
  public void markUnknown(String paymentId, String reason) {
    Payment payment = getPaymentWithLock(paymentId);
    if (!isCancellationInProgress(payment)) {
      return;
    }
    payment.markScheduleCancellationUnknown(reason, LocalDateTime.now());
  }

  @Transactional
  public void requireCompensation(
      String paymentId,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    Payment payment = getPaymentWithLock(paymentId);
    if (!isCancellationInProgress(payment)) {
      return;
    }
    payment.requireCompensationForScheduledPayment(
        payment.getProvider(),
        providerPaymentKey,
        paidAt,
        LocalDateTime.now()
    );
  }

  @Transactional
  public void completePaymentFailure(
      String paymentId,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    Payment payment = getPaymentWithLock(paymentId);
    if (!isCancellationInProgress(payment)) {
      return;
    }
    payment.scheduledPaymentFail(
        payment.getProvider(),
        failureCode,
        failureReason,
        failedAt
    );
  }

  private LocalDateTime currentPassExpireAt(Organization organization) {
    MembershipPass membershipPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization,
            MembershipPassStatus.ACTIVE
        )
        .orElse(null);
    return membershipPass != null ? membershipPass.getEndAt() : null;
  }

  private Payment getPaymentWithLock(String paymentId) {
    return paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
  }

  private boolean isCancellationInProgress(Payment payment) {
    return payment.isScheduleCancelPending() || payment.isScheduleCancelUnknown();
  }
}
