package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.membership.pay.compensation.PendingPaymentRecoveryService;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentRecoveryScheduler {

  private final PaymentRepository paymentRepository;
  private final PendingPaymentRecoveryService recoveryService;

  @Value("${scheduler.pending-payment-recovery.batch-size:50}")
  private int batchSize;

  @Value("${scheduler.pending-payment-recovery.retry-age-seconds:60}")
  private long retryAgeSeconds;

  @Scheduled(fixedDelayString = "${scheduler.pending-payment-recovery.delay:60000}")
  public void recoverPendingPayments() {
    List<String> paymentIds = paymentRepository.findPendingIdsForRecovery(
        PaymentStatus.PENDING,
        LocalDateTime.now().minusSeconds(retryAgeSeconds),
        PageRequest.of(0, batchSize)
    );
    for (String paymentId : paymentIds) {
      try {
        recoveryService.recover(paymentId);
      } catch (RuntimeException exception) {
        log.error("PENDING 결제 복구 실패. paymentId={}", paymentId, exception);
      }
    }
  }
}
