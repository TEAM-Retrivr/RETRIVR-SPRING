package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassExpirationService;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentPreparation;
import retrivr.retrivrspring.application.service.admin.membership.pay.immediate.ImmediatePaymentTransactionService;
import retrivr.retrivrspring.application.service.admin.membership.pay.portone.PortOnePaymentService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleCancellationService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
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
  private final MembershipPassExpirationService membershipPassExpirationService;
  private final PortOnePaymentService paymentService;
  private final ImmediatePaymentTransactionService immediatePaymentTransactionService;
  private final BillingScheduleRequestService billingScheduleRequestService;
  private final SubscriptionStartPreparationService startPreparationService;
  private final SubscriptionStartCompletionService startCompletionService;
  private final ScheduledSubscriptionStartService scheduledStartService;
  private final SubscriptionCancellationTransactionService cancellationTransactionService;
  private final BillingScheduleCancellationService billingScheduleCancellationService;
  private final SubscriptionPlanChangeTransactionService planChangeTransactionService;

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
        membershipPassExpirationService.processExpiredPass(loginOrganizationId, now);
      } else {
        SubscriptionStartResponse response = scheduledStartService.start(
            loginOrganizationId,
            lastRegisteredPass.getId(),
            request,
            now
        );
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

    ImmediatePaymentPreparation paymentPreparation =
        immediatePaymentTransactionService.prepare(preparation.subscriptionId());
    Payment payment = paymentService.charge(paymentPreparation, now);

    if (payment.isUnknown()) {
      throw new ApplicationException(ErrorCode.PAYMENT_CONFIRMATION_PENDING);
    }
    if (!payment.isSuccess()) {
      startCompletionService.failImmediatePayment(payment.getId());
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }

    SubscriptionStartResponse response;
    try {
      response = startCompletionService.completeImmediatePayment(payment.getId());
    } catch (RuntimeException exception) {
      startCompletionService.requireCompensation(payment.getId(), exception.getMessage());
      throw new ApplicationException(
          ErrorCode.SUBSCRIPTION_ACTIVATION_FAILED,
          "결제는 완료되었지만 구독 활성화에 실패하여 환불 처리가 필요합니다."
      );
    }

    requestNextBillingSafely(response);
    return response;
  }

  private void requestNextBillingSafely(SubscriptionStartResponse response) {
    requestBillingScheduleSafely(response.subscriptionId());
  }

  private void requestBillingScheduleSafely(String subscriptionId) {
    try {
      billingScheduleRequestService.request(subscriptionId);
    } catch (RuntimeException exception) {
      log.error(
          "다음 결제 예약 요청 실패. subscriptionId={}",
          subscriptionId,
          exception
      );
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public SubscriptionCancelResponse cancelSubscription(Long loginOrganizationId) {
    BillingScheduleCancellationPreparation preparation =
        cancellationTransactionService.prepare(
            loginOrganizationId,
            LocalDateTime.now()
        );

    billingScheduleCancellationService.cancel(preparation);

    return new SubscriptionCancelResponse(
        preparation.subscriptionId(),
        preparation.subscriptionStatus(),
        preparation.canceledAt(),
        preparation.currentPassExpireAt()
    );
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public SubscriptionPlanChangeResponse changeSubscriptionPlan(
      Long loginOrganizationId,
      SubscriptionPlanChangeRequest request
  ) {
    SubscriptionPlanChangePreparation preparation =
        planChangeTransactionService.prepare(
            loginOrganizationId,
            request.plan(),
            LocalDateTime.now()
        );

    boolean previousScheduleCanceled = billingScheduleCancellationService.cancel(
        preparation.cancellation()
    );
    if (previousScheduleCanceled) {
      requestBillingScheduleSafely(preparation.subscriptionId());
    }

    return new SubscriptionPlanChangeResponse(
        preparation.subscriptionId(),
        preparation.plan(),
        preparation.nextBillingAt()
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
