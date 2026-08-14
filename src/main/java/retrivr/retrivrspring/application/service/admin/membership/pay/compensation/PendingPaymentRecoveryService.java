package retrivr.retrivrspring.application.service.admin.membership.pay.compensation;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.application.service.admin.membership.pay.portone.PortOnePaymentService;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentPreparation;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentTransactionService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionStartCompletionService;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
public class PendingPaymentRecoveryService {

  private final ImmediatePaymentTransactionService transactionService;
  private final PortOnePaymentService paymentService;
  private final SubscriptionStartCompletionService completionService;
  private final BillingScheduleRequestService billingScheduleRequestService;

  public void recover(String paymentId) {
    ImmediatePaymentPreparation preparation = transactionService.prepareRetry(paymentId);
    Payment payment = paymentService.charge(preparation, LocalDateTime.now());

    if (payment.isUnknown()) {
      return;
    }
    if (payment.isFailed()) {
      completionService.failImmediatePayment(paymentId);
      return;
    }
    if (!payment.isSuccess()) {
      return;
    }

    SubscriptionStartResponse subscriptionResponse;
    try {
      subscriptionResponse = completionService.completeImmediatePayment(paymentId);
    } catch (RuntimeException exception) {
      completionService.requireCompensation(paymentId, exception.getMessage());
      return;
    }

    try {
      billingScheduleRequestService.request(
          subscriptionResponse.subscriptionId(),
          subscriptionResponse.nextBillingAt()
      );
    } catch (RuntimeException ignored) {
      // 누락 예약 스케줄러가 복구한다.
    }
  }
}
