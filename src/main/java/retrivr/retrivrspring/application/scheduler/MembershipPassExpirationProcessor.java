package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassExpirationService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipPassExpirationProcessor {

  private final MembershipPassExpirationService membershipPassExpirationService;
  private final MembershipPassRepository membershipPassRepository;

  @Transactional
  public void expireBatch() {
    LocalDateTime now = LocalDateTime.now();

    List<MembershipPass> expiredPasses =
        membershipPassRepository.findExpiredActivePassesForUpdate(
            MembershipPassStatus.ACTIVE,
            now
        );

    int expiredCount = 0;
    for (MembershipPass expiredPass : expiredPasses) {
      if (membershipPassExpirationService.processExpiredPass(expiredPass.getOrganization().getId(), now)) {
        expiredCount++;
      };
    }

    if (expiredCount > 0) {
      log.info("Expired {} membership passes", expiredCount);
    }
  }
}
