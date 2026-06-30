package retrivr.retrivrspring.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipPassExpirationScheduler {

  private final MembershipPassExpirationProcessor membershipPassExpirationProcessor;

  @Scheduled(cron = "0 */5 * * * *")
  public void processExpiredPass() {
    membershipPassExpirationProcessor.expireBatch();
  }
}
