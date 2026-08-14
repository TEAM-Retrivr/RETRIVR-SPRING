package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduleRetryScheduler {

  private static final List<PaymentStatus> TARGET_STATUSES = List.of(
      PaymentStatus.SCHEDULE_PENDING,
      PaymentStatus.SCHEDULE_UNKNOWN
  );

  private final PaymentRepository paymentRepository;
  private final BillingScheduleRequestService requestService;

  @Value("${scheduler.billing-schedule-retry.batch-size:50}")
  private int batchSize;

  @Value("${scheduler.billing-schedule-retry.retry-age-seconds:30}")
  private long retryAgeSeconds;

  @Scheduled(fixedDelayString = "${scheduler.billing-schedule-retry.delay:30000}")
  public void retrySchedules() {
    List<String> paymentIds = paymentRepository.findIdsForScheduleRetry(
        TARGET_STATUSES,
        LocalDateTime.now().minusSeconds(retryAgeSeconds),
        PageRequest.of(0, batchSize)
    );

    for (String paymentId : paymentIds) {
      try {
        requestService.retry(paymentId);
      } catch (RuntimeException exception) {
        log.error("결제 예약 재시도 실패. paymentId={}", paymentId, exception);
      }
    }
  }
}
