package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassExpirationService;
import retrivr.retrivrspring.application.service.admin.membership.pay.portone.PortOnePaymentService;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentPreparation;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentTransactionService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleCancellationService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
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
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionPlanChangeRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionCancelResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionPlanChangeResponse;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassExpirationService membershipPassExpirationService;
  private final PortOnePaymentService paymentService;
  private final ImmediatePaymentTransactionService immediatePaymentTransactionService;
  private final BillingScheduleRequestService billingScheduleRequestService;
  private final SubscriptionStartPreparationService startPreparationService;
  private final SubscriptionStartCompletionService startCompletionService;
  private final ScheduledSubscriptionStartService scheduledStartService;

  private final SubscriptionCancellationTransactionService cancellationTransactionService;
  private final BillingScheduleCancellationService billingScheduleCancellationService;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
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

    // 현재 마지막 등록(사용 대기중)된 패스.
    // 대기중인 패스가 없다면 현재 활성화된 패스.
    // 제작할 패스의 시작 시간을 결정하기 위해 조회.
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

    if (lastRegisteredPass != null) {
      if (lastRegisteredPass.isOverDue(now)) {
        // 현재 등록된 패스가 없고, ACTIVE 상태의 패스는 만료된 상태일 경우
        // 이후 즉시 결제를 진행
        membershipPassExpirationService.processExpiredPass(loginOrganizationId, now);
      }
      else {
        // 현재 등록된 패스가 ACTIVE  이지만 만료되지 않았을 경우
        // 혹은 REGISTERED 된 패스가 있을 경우
        SubscriptionStartResponse response = scheduledStartService.start(
            loginOrganizationId,
            lastRegisteredPass.getId(),
            request,
            now
        );
        // 예약 실패는 구독 시작을 실패시키지 않으며 SCHEDULE_UNKNOWN으로 재시도한다.
        requestNextBillingSafely(response);
        return response;
      }
    }

    SubscriptionStartPreparation preparation = startPreparationService.prepare(
        loginOrganizationId,
        request.paymentMethodId(),
        request.plan(),
        now
    );

    // Payment.PENDING을 먼저 커밋한 후 PortOne 결제를 수행한다.
    ImmediatePaymentPreparation paymentPreparation =
        immediatePaymentTransactionService.prepare(preparation.subscriptionId());
    Payment payment = paymentService.charge(paymentPreparation, now);

    if (payment.isUnknown()) {
      throw new ApplicationException(ErrorCode.PAYMENT_CONFIRMATION_PENDING);
    }

    // 결제 실패시 롤백 (결제 실패 정보는 저장됨 - paymentService.charge)
    if (!payment.isSuccess()) {
      startCompletionService.failImmediatePayment(payment.getId());
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }

    SubscriptionStartResponse response;
    try {
      response = startCompletionService.completeImmediatePayment(
          payment.getId()
      );
    } catch (RuntimeException exception) {
      startCompletionService.requireCompensation(
          payment.getId(),
          exception.getMessage()
      );
      throw new ApplicationException(
          ErrorCode.SUBSCRIPTION_ACTIVATION_FAILED,
          "결제는 완료되었지만 구독 활성화에 실패하여 환불 처리가 필요합니다."
      );
    }

    // 현재 결제와 구독 활성화가 커밋된 뒤 다음 결제를 예약한다.
    requestNextBillingSafely(response);

    return response;
  }

  private void requestNextBillingSafely(SubscriptionStartResponse response) {
    try {
      billingScheduleRequestService.request(
          response.subscriptionId(),
          response.nextBillingAt()
      );
    } catch (RuntimeException exception) {
      // 구독과 현재 이용권은 이미 커밋되었다. 누락 예약 스케줄러가 다시 생성한다.
      // SCHEDULE_UNKNOWN 형태의 Payment 가 남게 된다.
      log.error(
          "다음 결제 예약 요청 실패. subscriptionId={}",
          response.subscriptionId(),
          exception
      );
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public SubscriptionCancelResponse cancelSubscription(Long loginOrganizationId) {
    LocalDateTime now = LocalDateTime.now();

    BillingScheduleCancellationPreparation preparation =
        cancellationTransactionService.prepare(loginOrganizationId, now);

    billingScheduleCancellationService.cancel(preparation);

    return new SubscriptionCancelResponse(
        preparation.subscriptionId(),
        preparation.subscriptionStatus(),
        preparation.canceledAt(),
        preparation.currentPassExpireAt()
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
