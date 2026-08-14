package retrivr.retrivrspring.application.service.admin.membership.pay.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import retrivr.retrivrspring.application.event.BillingScheduleRequestedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduleEventListener {

  private final BillingScheduleRequestService requestService;

  @TransactionalEventListener(
      phase = TransactionPhase.AFTER_COMMIT,
      fallbackExecution = true
  )
  public void requestAfterCommit(BillingScheduleRequestedEvent event) {
    try {
      requestService.request(event.subscriptionId(), event.billingAt());
    } catch (RuntimeException exception) {
      // 예약 레코드 또는 누락 예약 스케줄러가 후속 복구한다.
      log.error(
          "다음 결제 예약 요청 실패. subscriptionId={}",
          event.subscriptionId(),
          exception
      );
    }
  }
}
