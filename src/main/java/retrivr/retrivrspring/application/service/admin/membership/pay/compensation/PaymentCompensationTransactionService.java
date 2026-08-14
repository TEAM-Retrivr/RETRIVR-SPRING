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
public class PaymentCompensationTransactionService {

  private final PaymentRepository paymentRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public boolean startRefund(String paymentId) {
    Payment payment = getPayment(paymentId);
    if (payment.isRefunded()) {
      return false;
    }
    payment.startRefund(LocalDateTime.now());
    return true;
  }

  @Transactional
  public void completeRefund(String paymentId) {
    Payment payment = getPayment(paymentId);
    if (payment.isRefunded()) {
      return;
    }
    payment.completeRefund(LocalDateTime.now());

    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElse(null);
    if (subscription != null && subscription.isStartPending()) {
      subscription.failStart(LocalDateTime.now());
    }
  }

  @Transactional
  public void markUnknown(String paymentId, String reason) {
    Payment payment = getPayment(paymentId);
    if (payment.isRefunded()) {
      return;
    }
    payment.markRefundUnknown(reason, LocalDateTime.now());
  }

  @Transactional
  public void failRefund(String paymentId, String reason) {
    Payment payment = getPayment(paymentId);
    if (payment.isRefunded()) {
      return;
    }
    payment.failRefund(reason, LocalDateTime.now());
  }

  private Payment getPayment(String paymentId) {
    return paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
  }
}
