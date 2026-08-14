package retrivr.retrivrspring.application.service.admin.membership.pay.compensation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;

@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

  private static final String REFUND_REASON = "구독 활성화 실패";

  private final PaymentRepository paymentRepository;
  private final PaymentCompensationTransactionService transactionService;
  private final PortOneClient portOneClient;

  public void compensate(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId).orElse(null);
    if (payment == null || payment.isRefunded()) {
      return;
    }

    if ((payment.isRefundProcessing() || payment.isRefundUnknown()) && verifyRefunded(paymentId)) {
      transactionService.completeRefund(paymentId);
      return;
    }

    if (!transactionService.startRefund(paymentId)) {
      return;
    }

    try {
      PortOneCancelPaymentResponse response = portOneClient.cancelPayment(
          paymentId,
          new PortOneCancelPaymentRequest(REFUND_REASON)
      );
      handleResponse(paymentId, response);
    } catch (PortOneException exception) {
      if (verifyRefunded(paymentId)) {
        transactionService.completeRefund(paymentId);
      } else {
        transactionService.markUnknown(paymentId, exception.getMessage());
      }
    }
  }

  private void handleResponse(String paymentId, PortOneCancelPaymentResponse response) {
    if (response == null || response.cancellation() == null) {
      transactionService.markUnknown(paymentId, "PortOne 환불 응답이 비어 있습니다.");
      return;
    }

    String status = response.cancellation().status();
    if ("SUCCEEDED".equals(status)) {
      transactionService.completeRefund(paymentId);
    } else if ("FAILED".equals(status)) {
      transactionService.failRefund(paymentId, "PortOne에서 환불 실패 상태를 반환했습니다.");
    } else {
      transactionService.markUnknown(
          paymentId,
          "PortOne 환불 상태가 아직 확정되지 않았습니다. status=" + status
      );
    }
  }

  private boolean verifyRefunded(String paymentId) {
    try {
      PortOnePaymentResponse response = portOneClient.getPayment(paymentId);
      return response != null && response.isCancelled();
    } catch (PortOneException exception) {
      return false;
    }
  }
}
