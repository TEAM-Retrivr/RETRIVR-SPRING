package retrivr.retrivrspring.application.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissingBillingScheduleScheduler {

  private static final List<PaymentStatus> SCHEDULE_STATUSES = List.of(
      PaymentStatus.SCHEDULE_PENDING,
      PaymentStatus.SCHEDULE_UNKNOWN,
      PaymentStatus.SCHEDULED
  );

  private final SubscriptionRepository subscriptionRepository;
  private final BillingScheduleRequestService requestService;

  @Value("${scheduler.missing-billing-schedule.batch-size:50}")
  private int batchSize;

  @Scheduled(fixedDelayString = "${scheduler.missing-billing-schedule.delay:60000}")
  public void recoverMissingSchedules() {
    List<String> subscriptionIds = subscriptionRepository.findIdsMissingBillingSchedule(
        SubscriptionStatus.ACTIVE,
        SCHEDULE_STATUSES,
        PageRequest.of(0, batchSize)
    );

    for (String subscriptionId : subscriptionIds) {
      try {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription != null && subscription.getNextBillingAt() != null) {
          requestService.request(subscriptionId, subscription.getNextBillingAt());
        }
      } catch (RuntimeException exception) {
        log.error("누락된 결제 예약 복구 실패. subscriptionId={}", subscriptionId, exception);
      }
    }
  }
}
