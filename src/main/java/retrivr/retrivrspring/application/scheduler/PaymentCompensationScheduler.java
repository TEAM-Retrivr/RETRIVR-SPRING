package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.membership.pay.compensation.PaymentCompensationService;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationScheduler {

  private static final List<PaymentStatus> TARGET_STATUSES = List.of(
      PaymentStatus.COMPENSATION_REQUIRED,
      PaymentStatus.REFUND_PROCESSING,
      PaymentStatus.REFUND_UNKNOWN
  );

  private final PaymentRepository paymentRepository;
  private final PaymentCompensationService compensationService;

  @Value("${scheduler.payment-compensation.batch-size:50}")
  private int batchSize;

  @Value("${scheduler.payment-compensation.retry-age-seconds:30}")
  private long retryAgeSeconds;

  @Scheduled(fixedDelayString = "${scheduler.payment-compensation.delay:30000}")
  public void compensatePayments() {
    List<String> paymentIds = paymentRepository.findIdsForCompensation(
        TARGET_STATUSES,
        LocalDateTime.now().minusSeconds(retryAgeSeconds),
        PageRequest.of(0, batchSize)
    );

    for (String paymentId : paymentIds) {
      try {
        compensationService.compensate(paymentId);
      } catch (RuntimeException exception) {
        log.error("결제 보상 처리 실패. paymentId={}", paymentId, exception);
      }
    }
  }
}
