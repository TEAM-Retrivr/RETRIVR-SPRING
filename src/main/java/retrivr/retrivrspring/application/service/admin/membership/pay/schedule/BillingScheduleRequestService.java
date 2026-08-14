package retrivr.retrivrspring.application.service.admin.membership.pay.schedule;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentAmountRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentResponse;

@Service
@RequiredArgsConstructor
public class BillingScheduleRequestService {

  private static final String CURRENCY_KRW = "KRW";

  private final BillingScheduleTransactionService transactionService;
  private final PortOneClient portOneClient;

  public void request(String subscriptionId, java.time.LocalDateTime billingAt) {
    execute(transactionService.prepare(subscriptionId, billingAt));
  }

  public void retry(String paymentId) {
    execute(transactionService.prepareRetry(paymentId));
  }

  private void execute(BillingSchedulePreparation preparation) {
    if (preparation.alreadyScheduled()) {
      return;
    }

    try {
      // 실제 PortOne 결제 예약 요청
      PortOneScheduleBillingPaymentResponse response = portOneClient.scheduleBillingPayment(
          createRequest(preparation),
          preparation.provider()
      );

      if (response == null || response.schedule() == null || response.schedule().id() == null) {
        transactionService.markUnknown(
            preparation.paymentId(),
            "PortOne 결제 예약 응답이 비어 있습니다."
        );
        return;
      }

      transactionService.complete(
          preparation.paymentId(),
          response.schedule().id(),
          preparation.billingAt()
      );

    } catch (PortOneException exception) {
      transactionService.markUnknown(preparation.paymentId(), exception.getMessage());
    }
  }

  private PortOneScheduleBillingPaymentRequest createRequest(
      BillingSchedulePreparation preparation
  ) {
    OffsetDateTime timeToPay = preparation.billingAt()
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime();
    return new PortOneScheduleBillingPaymentRequest(
        preparation.paymentId(),
        null,
        preparation.billingKey(),
        null,
        "Retrivr " + preparation.plan().getKorean() + " 구독",
        preparation.customer(),
        null,
        PortOnePaymentAmountRequest.total(preparation.amount()),
        CURRENCY_KRW,
        null,
        null,
        1,
        timeToPay
    );
  }
}
