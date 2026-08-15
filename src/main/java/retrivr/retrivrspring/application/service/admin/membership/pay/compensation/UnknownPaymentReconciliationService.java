package retrivr.retrivrspring.application.service.admin.membership.pay.compensation;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionStartCompletionService;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
public class UnknownPaymentReconciliationService {

  private final PaymentRepository paymentRepository;
  private final PortOneClient portOneClient;
  private final PaymentReconciliationTransactionService transactionService;
  private final SubscriptionStartCompletionService completionService;
  private final BillingScheduleRequestService billingScheduleRequestService;

  public void reconcile(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId).orElse(null);
    if (payment == null || !payment.isUnknown()) {
      return;
    }

    PortOnePaymentResponse response;
    try {
      response = portOneClient.getPayment(paymentId);
    } catch (PortOneException exception) {
      transactionService.defer(paymentId, exception.getMessage());
      return;
    }

    if (!isSamePayment(payment, response)) {
      transactionService.defer(paymentId, "PortOne 결제 조회 결과가 요청 정보와 일치하지 않습니다.");
      return;
    }

    if (response.isPaid()) {
      resolveSuccess(paymentId, response);
      return;
    }

    if (response.isFailed()) {
      resolveFailure(paymentId, response);
      return;
    }

    transactionService.defer(
        paymentId,
        "PortOne 결제 상태가 아직 확정되지 않았습니다. status=" + response.status()
    );
  }

  private void resolveSuccess(String paymentId, PortOnePaymentResponse response) {
    LocalDateTime paidAt = response.paidAt() != null
        ? response.paidAt().toLocalDateTime()
        : LocalDateTime.now();

    boolean resolved = transactionService.resolveSuccess(
        paymentId,
        response.scheduleId(),
        response.transactionId(),
        paidAt
    );
    if (!resolved) {
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
      billingScheduleRequestService.request(subscriptionResponse.subscriptionId());
    } catch (RuntimeException ignored) {
      // 누락 예약 스케줄러가 복구한다.
    }
  }

  private void resolveFailure(String paymentId, PortOnePaymentResponse response) {
    String failureCode = response.failure() != null && response.failure().pgCode() != null
        ? response.failure().pgCode()
        : "PORTONE_PAYMENT_FAILED";
    String failureReason = response.failure() != null
        ? response.failure().reason()
        : "PortOne에서 결제 실패 상태를 반환했습니다.";
    LocalDateTime failedAt = response.failedAt() != null
        ? response.failedAt().toLocalDateTime()
        : LocalDateTime.now();

    boolean resolved = transactionService.resolveFailure(
        paymentId,
        failureCode,
        failureReason,
        failedAt
    );
    if (resolved) {
      completionService.failImmediatePayment(paymentId);
    }
  }

  private boolean isSamePayment(Payment payment, PortOnePaymentResponse response) {
    return response != null
        && response.hasPaymentId(payment.getId())
        && response.hasTotalAmount(payment.getAmount());
  }
}
