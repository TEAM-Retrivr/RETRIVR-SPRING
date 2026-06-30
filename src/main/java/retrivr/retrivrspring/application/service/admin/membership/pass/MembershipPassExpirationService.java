package retrivr.retrivrspring.application.service.admin.membership.pass;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionBillingService;
import retrivr.retrivrspring.application.vo.BillingResult;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipPassExpirationService {

  private final MembershipPassRepository membershipPassRepository;
  private final SubscriptionBillingService subscriptionBillingService;

  @Transactional
  public void processExpiredPass(MembershipPass expiredPass, LocalDateTime now) {
    Organization organization = expiredPass.getOrganization();

    MembershipPass nextPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceAsc(
            organization,
            MembershipPassStatus.REGISTERED
        )
        .orElse(null);

    // 다음 패스가 존재한다면 활성화한 후 기존 패스 만료시킴
    if (nextPass != null) {
      nextPass.activate(now);
      expiredPass.expire(now);
      return;
    }

    // 결제 성공 혹은 했다면 기존 패스 만료시킴
    BillingResult result = subscriptionBillingService.billIfAvailable(organization, now);
    if (result != BillingResult.PAYMENT_RETRYABLE_FAILED) {
      expiredPass.expire(now);
    }
  }
}
