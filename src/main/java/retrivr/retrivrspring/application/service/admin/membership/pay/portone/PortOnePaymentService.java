package retrivr.retrivrspring.application.service.admin.membership.pay.portone;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentPreparation;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentTransactionService;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentAmountRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortOnePaymentService {

  private static final String CURRENCY_KRW = "KRW";

  private final PortOneClient portOneClient;
  private final ImmediatePaymentTransactionService immediatePaymentTransactionService;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Payment charge(
      ImmediatePaymentPreparation preparation,
      LocalDateTime now
  ) {
    String paymentId = preparation.paymentId();

    PortOneBillingKeyPaymentResponse response = null;
    String requestError = null;

    try {
      response = portOneClient.chargeBillingKey(
          new PortOneBillingKeyPaymentRequest(
              paymentId,
              null,
              preparation.billingKey(),
              null,
              orderName(preparation.plan()),
              preparation.customer(),
              null,
              PortOnePaymentAmountRequest.total(preparation.amount()),
              CURRENCY_KRW,
              null,
              null,
              1,
              false
          ),
          preparation.provider()
      );
    } catch (PortOneException e) {
      // 요청 오류만으로 결제 실패를 확정할 수 없다. 동일 paymentId를 조회해 최종 상태를 판단한다.
      requestError = e.getMessage();
    }

    PortOnePaymentResponse portOnePayment;
    try {
      portOnePayment = portOneClient.getPayment(paymentId);
    } catch (PortOneException e) {
      return immediatePaymentTransactionService.markUnknown(
          paymentId,
          resolveUnknownReason(requestError, e.getMessage()),
          now
      );
    }

    if (!isVerifiablePayment(portOnePayment, paymentId, preparation.amount())) {
      return immediatePaymentTransactionService.markUnknown(
          paymentId,
          resolveUnknownReason(requestError, "PortOne 결제 조회 결과가 요청 정보와 일치하지 않습니다."),
          now
      );
    }

    if (portOnePayment.isPaid()) {
      LocalDateTime paidAt = resolvePaidAt(response, portOnePayment, now);
      return immediatePaymentTransactionService.completeSuccess(
          paymentId,
          portOnePayment.scheduleId(),
          providerPaymentKey(response, portOnePayment),
          paidAt
      );
    }

    if (portOnePayment.isFailed()) {
      return immediatePaymentTransactionService.completeFailure(
          paymentId,
          failureCode(portOnePayment),
          failureReason(portOnePayment),
          portOnePayment.failedAt() != null ? portOnePayment.failedAt().toLocalDateTime() : now
      );
    }

    return immediatePaymentTransactionService.markUnknown(
        paymentId,
        "PortOne 결제 상태가 아직 확정되지 않았습니다. status=" + portOnePayment.status(),
        now
    );
  }

  public PortOnePaymentResponse getVerifiedPayment(String paymentId, long expectedAmount) {
    PortOnePaymentResponse payment = portOneClient.getPayment(paymentId);
    if (payment == null || !payment.hasPaymentId(paymentId)) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }
    if (!payment.hasTotalAmount(expectedAmount)) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }
    return payment;
  }

  private String orderName(SubscriptionPlan plan) {
    return "Retrivr " + plan.getKorean() + " 구독";
  }

  private LocalDateTime resolvePaidAt(
      PortOneBillingKeyPaymentResponse response,
      PortOnePaymentResponse payment,
      LocalDateTime fallback
  ) {
    if (response != null && response.payment() != null && response.payment().paidAt() != null) {
      return response.payment().paidAt().toLocalDateTime();
    }
    if (payment != null && payment.paidAt() != null) {
      return payment.paidAt().toLocalDateTime();
    }
    return fallback;
  }

  private boolean isVerifiablePayment(
      PortOnePaymentResponse payment,
      String paymentId,
      long expectedAmount
  ) {
    return payment != null
        && payment.hasPaymentId(paymentId)
        && payment.hasTotalAmount(expectedAmount);
  }

  private String failureCode(PortOnePaymentResponse payment) {
    if (payment.failure() == null || payment.failure().pgCode() == null) {
      return "PORTONE_PAYMENT_FAILED";
    }
    return payment.failure().pgCode();
  }

  private String failureReason(PortOnePaymentResponse payment) {
    if (payment.failure() == null) {
      return "PortOne에서 결제 실패 상태를 반환했습니다.";
    }
    if (payment.failure().reason() != null) {
      return payment.failure().reason();
    }
    return payment.failure().pgMessage();
  }

  private String providerPaymentKey(
      PortOneBillingKeyPaymentResponse response,
      PortOnePaymentResponse payment
  ) {
    if (response != null && response.payment() != null && response.payment().pgTxId() != null) {
      return response.payment().pgTxId();
    }
    return payment.transactionId();
  }

  private String resolveUnknownReason(String requestError, String verificationError) {
    if (requestError == null || requestError.isBlank()) {
      return verificationError;
    }
    return requestError + " / 결제 조회: " + verificationError;
  }

}
