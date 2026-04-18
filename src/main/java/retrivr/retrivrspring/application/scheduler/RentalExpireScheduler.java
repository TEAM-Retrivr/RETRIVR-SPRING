package retrivr.retrivrspring.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalExpireScheduler {

  @Value("${scheduler.rental-expire.batch-size}")
  private static int BATCH_SIZE;
  private final RentalExpireProcessor rentalExpireProcessor;

  @Scheduled(fixedDelayString = "${scheduler.rental-expire.delay}")
  public void expireRequestedRentals() {
    int totalProcessed = 0;

    while (true) {
      int processed = rentalExpireProcessor.expireBatch(BATCH_SIZE);
      totalProcessed += processed;

      // 100개씩 만료된 요청들을 거절시킴
      // processed < BATCH_SIZE : 100개보다 적을 경우, 다음 루프에서 거절할 요청이 없으므로 종료
      if (processed < BATCH_SIZE) {
        break;
      }
    }

    if (totalProcessed > 0) {
      log.info("[System] 자동 만료 처리 완료. processed={}", totalProcessed);
    }
  }
}
