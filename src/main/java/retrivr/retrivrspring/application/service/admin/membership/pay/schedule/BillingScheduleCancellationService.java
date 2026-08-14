package retrivr.retrivrspring.application.service.admin.membership.pay.schedule;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.application.service.admin.membership.subscription.BillingScheduleCancellationPreparation;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionCancellationTransactionService;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;

@Service
@RequiredArgsConstructor
public class BillingScheduleCancellationService {

  private final PortOneClient portOneClient;
  private final SubscriptionCancellationTransactionService transactionService;

  public void retry(String paymentId) {
    cancel(transactionService.prepareRetry(paymentId));
  }

  public void cancel(BillingScheduleCancellationPreparation preparation) {
    if (!preparation.cancellationRequired()) {
      return;
    }
    if (preparation.scheduleId() == null || preparation.scheduleId().isBlank()) {
      transactionService.markUnknown(
          preparation.paymentId(),
          "취소할 PortOne scheduleId가 없습니다."
      );
      return;
    }

    try {
      PortOneCancelScheduledPaymentResponse response =
          portOneClient.cancelScheduledPayment(
              PortOneCancelScheduledPaymentRequest.byScheduleId(
                  preparation.scheduleId()
              )
          );

      if (response == null
          || response.revokedScheduleIds() == null
          || !response.revokedScheduleIds().contains(preparation.scheduleId())) {
        transactionService.markUnknown(
            preparation.paymentId(),
            "PortOne 예약 취소 응답에서 요청한 scheduleId를 확인할 수 없습니다."
        );
        return;
      }

      LocalDateTime canceledAt =
          response.revokedAt() != null
              ? response.revokedAt().toLocalDateTime()
              : LocalDateTime.now();

      transactionService.complete(
          preparation.paymentId(),
          canceledAt
      );
    } catch (PortOneException exception) {
      reconcileProcessedPayment(preparation, exception);
    }
  }

  private void reconcileProcessedPayment(
      BillingScheduleCancellationPreparation preparation,
      PortOneException cancellationException
  ) {
    PortOnePaymentResponse payment;
    try {
      payment = portOneClient.getPayment(preparation.paymentId());
    } catch (PortOneException verificationException) {
      transactionService.markUnknown(
          preparation.paymentId(),
          cancellationException.getMessage()
              + " / 결제 조회: "
              + verificationException.getMessage()
      );
      return;
    }

    if (payment == null
        || !payment.hasPaymentId(preparation.paymentId())
        || preparation.amount() == null
        || !payment.hasTotalAmount(preparation.amount())) {
      transactionService.markUnknown(
          preparation.paymentId(),
          "예약 취소 실패 후 조회한 결제 정보가 요청과 일치하지 않습니다."
      );
      return;
    }

    if (payment.isPaid()) {
      transactionService.requireCompensation(
          preparation.paymentId(),
          payment.transactionId(),
          payment.paidAt() != null
              ? payment.paidAt().toLocalDateTime()
              : LocalDateTime.now()
      );
      return;
    }

    if (payment.isFailed()) {
      transactionService.completePaymentFailure(
          preparation.paymentId(),
          payment.failure() != null && payment.failure().pgCode() != null
              ? payment.failure().pgCode()
              : "PORTONE_SCHEDULED_PAYMENT_FAILED",
          payment.failure() != null && payment.failure().reason() != null
              ? payment.failure().reason()
              : "예약 취소 처리 중 결제가 실패했습니다.",
          payment.failedAt() != null
              ? payment.failedAt().toLocalDateTime()
              : LocalDateTime.now()
      );
      return;
    }

    transactionService.markUnknown(
        preparation.paymentId(),
        "예약 취소 결과와 결제 결과가 아직 확정되지 않았습니다. status=" + payment.status()
    );
  }
}
