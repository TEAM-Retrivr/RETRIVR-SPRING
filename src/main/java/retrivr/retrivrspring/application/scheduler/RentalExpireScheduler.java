package retrivr.retrivrspring.application.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RentalExpireScheduler {

  private final int BATCH_SIZE;
  private final int MAX_ITERATIONS; // 비정상 무한 루프 방지용 상한. 최대 BATCH_SIZE * MAX_ITERATIONS 만큼의 row 처리
  private final RentalExpireProcessor rentalExpireProcessor;

  public RentalExpireScheduler(
      @Value("${scheduler.rental-expire.batch-size}")
      int batchSize,
      @Value("${scheduler.rental-expire.max-iterations}")
      int maxIterations,
      RentalExpireProcessor rentalExpireProcessor
  ) {
    this.rentalExpireProcessor = rentalExpireProcessor;
    this.BATCH_SIZE = batchSize;
    this.MAX_ITERATIONS = maxIterations;
  }

  @Scheduled(fixedDelayString = "${scheduler.rental-expire.delay}")
  public void expireRequestedRentals() {
    int totalProcessed = 0;

    for (int i = 0; i < MAX_ITERATIONS; i++) {
      int processed = rentalExpireProcessor.expireBatch(BATCH_SIZE);
      totalProcessed += processed;

      if (processed < BATCH_SIZE) {
        break;
      }

      if (i == MAX_ITERATIONS - 1) {
        log.warn("자동 만료 처리 중 최대 반복 횟수 도달. totalProcessed={}", totalProcessed);
      }
    }

    if (totalProcessed > 0) {
      log.info("[System] 자동 만료 처리 완료. processed={}", totalProcessed);
    }
  }
}
