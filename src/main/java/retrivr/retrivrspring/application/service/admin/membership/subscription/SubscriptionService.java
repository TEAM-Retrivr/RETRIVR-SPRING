package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.application.service.admin.membership.pay.PaymentMethodService;
import retrivr.retrivrspring.application.service.admin.membership.pay.PortOnePaymentService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentMethodRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionPlanChangeRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionCancelResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionPlanChangeResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassService membershipPassService;
  private final PortOnePaymentService paymentService;
  private final PaymentMethodService paymentMethodService;

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

    // 결제수단 정보 가져오기
    PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
        .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION, "결제 수단을 찾을 수 없습니다."));

    // 현재 마지막 등록(사용 대기중)된 패스.
    // 대기중인 패스가 없다면 현재 활성화된 패스.
    MembershipPass lastRegisteredPass = membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization,
            MembershipPassStatus.REGISTERED
        )
        .orElse(
            membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceAsc(
                    organization,
                    MembershipPassStatus.ACTIVE
                )
                .orElse(null)
        );

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElse(null);

    // 구독 정보 생성
    if (subscription == null) {
      // 구독정보가 없을 경우 생성
      subscription = Subscription.start(organization, request.plan(), now);
      subscriptionRepository.save(subscription);
    } else if (subscription.isPaused()) {
      // 구독이 중단되었을 경우 재시작
      subscription.restart(
          organization,
          request.plan(),
          now,
          lastRegisteredPass != null ? lastRegisteredPass.getEndAt() : null
      );
    } else if (subscription.isActive()) {
      // 이미 구독중인 경우
      throw new ApplicationException(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    }

    // 결제수단 변경
    paymentMethodService.changeDefaultPaymentMethod(loginOrganizationId, paymentMethod.getId());

    if (lastRegisteredPass != null) {
      if (lastRegisteredPass.isOverDue(now)) {
        lastRegisteredPass.expire(now);
      }
      else {
        // 결제 예약
        paymentService.scheduleBillingPayment(subscription, subscription.getNextBillingAt());
        subscription.scheduleNextBillingAt(lastRegisteredPass.getEndAt());
        return new SubscriptionStartResponse(
            subscription.getId(),
            subscription.getPlan(),
            subscription.getStatus(),
            subscription.getNextBillingAt(),
            lastRegisteredPass.getId(),
            lastRegisteredPass.getStartAt(),
            lastRegisteredPass.getEndAt()
        );
      }
    }

    // 즉시 결제
    Payment payment = paymentService.charge(
        organization,
        subscription,
        subscription.getPlan(),
        now
    );

    // 결제 실패시 롤백 (결제 실패 정보는 저장됨 - paymentService.charge)
    if (!payment.isSuccess()) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }

    subscription.completeSuccessfulPayment(now);

    MembershipPass membershipPass = membershipPassService.generateSubscriptionMembershipPass(
        loginOrganizationId,
        subscription
    );

    // 결제 예약
    paymentService.scheduleBillingPayment(subscription, subscription.getNextBillingAt());
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

    // 만약 예약결제가 적용되어 있다면 취소

    Payment pendingPayment = paymentRepository.findByOrganizationAndStatus(organization,
        PaymentStatus.SCHEDULED)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SCHEDULED_PAYMENT));

    paymentService.cancelScheduledPayment(pendingPayment);
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

  @Transactional
  public SubscriptionPlanChangeResponse changeSubscriptionPlan(
      Long loginOrganizationId,
      SubscriptionPlanChangeRequest request
  ) {
    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION));
    subscription.validateOwner(organization);

    if (!subscription.isActive()) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION);
    }

    if (subscription.matchesPlan(request.plan())) {
      throw new ApplicationException(ErrorCode.ALREADY_SAME_SUBSCRIPTION_PLAN);
    }

    Optional<Payment> opPendingPayment = paymentRepository.findByOrganizationAndStatus(
            organization,
            PaymentStatus.SCHEDULED
        );

    if (opPendingPayment.isPresent()) {
      paymentService.cancelScheduledPayment(opPendingPayment.get());
      subscription.changePlan(request.plan());
      paymentService.scheduleBillingPayment(subscription, subscription.getNextBillingAt());
    }
    else {
      subscription.changePlan(request.plan());
    }

    return new SubscriptionPlanChangeResponse(
        subscription.getId(),
        subscription.getPlan(),
        subscription.getNextBillingAt()
    );
  }

  @Transactional
  public void cancelSubscriptionWhenPaymentFail(Organization organization, LocalDateTime now) {
    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION));

    subscription.validateOwner(organization);

    if (!subscription.isActive()) {
      return;
    }
    subscription.cancel(organization, now);
  }
}
