package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.application.service.admin.membership.pay.PaymentService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionCancelResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final MembershipPassService membershipPassService;
  private final PaymentService paymentService;

  @Transactional
  public SubscriptionStartResponse startSubscription(
      Long loginOrganizationId,
      SubscriptionStartRequest request
  ) {
    LocalDateTime now = LocalDateTime.now();
    if (request.plan() == null) {
      throw new ApplicationException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }

    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElse(null);

    MembershipPass currentPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceAsc(
            organization,
            MembershipPassStatus.ACTIVE
        )
        .orElse(null);

    if (subscription == null) {
      subscription = Subscription.start(organization, request.plan(), now);
      subscriptionRepository.save(subscription);
    } else if (subscription.isPaused()) {
      subscription.restart(
          organization,
          request.plan(),
          now,
          currentPass != null ? currentPass.getEndAt() : null
      );
    } else if (subscription.isActive()) {
      throw new ApplicationException(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    }

    Payment payment = paymentService.manualPayment(
        organization,
        subscription,
        subscription.getPlan(),
        now
    );
    if (!payment.isSuccess()) {
      subscription.failPayment(now);
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }

    subscription.completeSuccessfulPayment();

    MembershipPass membershipPass = membershipPassService.generateSubscriptionMembershipPass(
        loginOrganizationId,
        subscription
    );
    subscription.scheduleNextBillingAt(membershipPass.getEndAt());

    return new SubscriptionStartResponse(
        subscription.getId(),
        subscription.getPlan(),
        subscription.getStatus(),
        subscription.getNextBillingAt(),
        membershipPass.getId(),
        membershipPass.getStartAt(),
        membershipPass.getEndAt()
    );
  }

  @Transactional
  public SubscriptionCancelResponse cancelSubscription(Long loginOrganizationId) {
    LocalDateTime now = LocalDateTime.now();
    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION));

    subscription.validateOwner(organization);

    if (!subscription.isActive()) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION);
    }

    subscription.cancel(organization, now);

    MembershipPass membershipPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization,
            MembershipPassStatus.ACTIVE
        )
        .orElse(null);

    return new SubscriptionCancelResponse(
        subscription.getId(),
        subscription.getStatus(),
        subscription.getCanceledAt(),
        membershipPass != null ? membershipPass.getEndAt() : null
    );
  }
}
