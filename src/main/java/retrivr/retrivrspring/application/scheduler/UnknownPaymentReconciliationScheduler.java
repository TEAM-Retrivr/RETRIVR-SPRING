package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.membership.pay.compensation.UnknownPaymentReconciliationService;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnknownPaymentReconciliationScheduler {

  private final PaymentRepository paymentRepository;
  private final UnknownPaymentReconciliationService reconciliationService;

  @Value("${scheduler.payment-reconciliation.batch-size:50}")
  private int batchSize;

  @Value("${scheduler.payment-reconciliation.retry-age-seconds:30}")
  private long retryAgeSeconds;

  @Scheduled(fixedDelayString = "${scheduler.payment-reconciliation.delay:30000}")
  public void reconcileUnknownPayments() {
    LocalDateTime retryBefore = LocalDateTime.now().minusSeconds(retryAgeSeconds);
    List<String> paymentIds = paymentRepository.findIdsForReconciliation(
        PaymentStatus.UNKNOWN,
        retryBefore,
        PageRequest.of(0, batchSize)
    );

    for (String paymentId : paymentIds) {
      try {
        reconciliationService.reconcile(paymentId);
      } catch (RuntimeException exception) {
        // 한 결제의 처리 실패가 다른 UNKNOWN 결제의 확인을 막지 않도록 한다.
        log.error("UNKNOWN 결제 재확인 실패. paymentId={}", paymentId, exception);
      }
    }
  }
}
