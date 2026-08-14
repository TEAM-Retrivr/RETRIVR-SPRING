package retrivr.retrivrspring.application.service.admin.membership.pay.compensation;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationTransactionService {

  private final PaymentRepository paymentRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public boolean resolveSuccess(
      String paymentId,
      String portOneScheduleId,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    Payment payment = getPaymentWithLock(paymentId);
    if (payment.isSuccess()) {
      return true;
    }
    if (!payment.isUnknown()) {
      return false;
    }

    payment.resolveUnknownAsSuccess(
        portOneScheduleId,
        providerPaymentKey,
        paidAt
    );
    return true;
  }

  @Transactional
  public boolean resolveFailure(
      String paymentId,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    Payment payment = getPaymentWithLock(paymentId);
    if (payment.isFailed()) {
      return true;
    }
    if (!payment.isUnknown()) {
      return false;
    }

    payment.resolveUnknownAsFailed(
        failureCode,
        failureReason,
        failedAt
    );
    Subscription subscription = subscriptionRepository
        .findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    if (subscription.isStartPending()) {
      subscription.failStart(failedAt);
    }
    return true;
  }

  @Transactional
  public void defer(String paymentId, String reason) {
    Payment payment = getPaymentWithLock(paymentId);
    payment.deferUnknownReconciliation(reason, LocalDateTime.now());
  }

  private Payment getPaymentWithLock(String paymentId) {
    return paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
  }
}
