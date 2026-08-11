package retrivr.retrivrspring.application.service.admin.membership.pass;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.event.ScheduledPaymentReconcileRequestedEvent;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipPassExpirationService {

  private final MembershipPassRepository membershipPassRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final OrganizationRepository organizationRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassService membershipPassService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public boolean processExpiredPass(Long organizationId, LocalDateTime now) {
    Organization lockedOrganization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    MembershipPass expiredPass = membershipPassRepository
        .findFirstByOrganizationAndStatusAndEndAtLessThanEqualOrderByEndAtAsc(
            lockedOrganization,
            MembershipPassStatus.ACTIVE,
            now
        )
        .orElse(null);

    if (expiredPass == null) {
      return false;
    }

    MembershipPass nextPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceAsc(
            lockedOrganization,
            MembershipPassStatus.REGISTERED
        )
        .orElse(null);

    // 다음 패스가 존재한다면 활성화한 후 기존 패스 만료시킴
    if (nextPass != null) {
      nextPass.activate(now);
      expiredPass.expire(now);
      return true;
    }

    expiredPass.expire(now);

    // 예약 결제 건에 대한 검증 및 다음 패스 제작
    paymentRepository.findByOrganizationAndStatus(lockedOrganization,
        PaymentStatus.SCHEDULED)
        .ifPresent(
        pendingPayment -> eventPublisher.publishEvent(
            new ScheduledPaymentReconcileRequestedEvent(pendingPayment.getId())
          )
        );
    return true;
  }

  @Transactional
  public Subscription processExpireCurrentPassWhenPaymentSuccess(Organization organization, Payment payment, LocalDateTime now) {
    MembershipPass currentPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization,
            MembershipPassStatus.ACTIVE
        )
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_PASS));

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));

    currentPass.expire(now);

    MembershipPass membershipPass = membershipPassService.generateSubscriptionMembershipPassWithPayment(
        organization.getId(),
        payment,
        subscription
    );

    subscription.scheduleNextBillingAt(membershipPass.getEndAt());
    return subscription;
  }
}
