package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanChangeTransactionService {

  private static final List<PaymentStatus> WORKFLOW_STATUSES = List.of(
      PaymentStatus.SCHEDULE_PENDING,
      PaymentStatus.SCHEDULE_UNKNOWN,
      PaymentStatus.SCHEDULED,
      PaymentStatus.SCHEDULE_CANCEL_PENDING,
      PaymentStatus.SCHEDULE_CANCEL_UNKNOWN,
      PaymentStatus.COMPENSATION_REQUIRED,
      PaymentStatus.REFUND_PROCESSING,
      PaymentStatus.REFUND_UNKNOWN,
      PaymentStatus.REFUND_FAILED
  );

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;

  @Transactional
  public SubscriptionPlanChangePreparation prepare(
      Long organizationId,
      SubscriptionPlan newPlan,
      LocalDateTime now
  ) {
    if (newPlan == null) {
      throw new ApplicationException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }

    Organization organization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION));
    subscription.validateOwner(organization);

    if (!subscription.isActive()) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_SUBSCRIPTION);
    }

    Payment workflowPayment = paymentRepository
        .findFirstByOrganizationAndStatusInOrderByCreatedAtDesc(
            organization,
            WORKFLOW_STATUSES
        )
        .orElse(null);

    if (subscription.matchesPlan(newPlan)) {
      return prepareSamePlanRecovery(subscription, workflowPayment);
    }

    if (workflowPayment != null && !workflowPayment.isScheduled()) {
      throw new ApplicationException(
          ErrorCode.SUBSCRIPTION_STATUS_CONFLICT,
          "기존 결제 예약 처리가 끝난 후 플랜을 변경할 수 있습니다."
      );
    }

    subscription.changePlan(newPlan);

    BillingScheduleCancellationPreparation cancellation =
        BillingScheduleCancellationPreparation.notRequired(
            null,
            subscription.getId(),
            subscription.getStatus(),
            null,
            null
        );

    if (workflowPayment != null) {
      workflowPayment.requestScheduleCancellation(now);
      cancellation = BillingScheduleCancellationPreparation.cancellationRequired(
          subscription,
          workflowPayment,
          null
      );
    }

    return toPreparation(subscription, cancellation);
  }

  private SubscriptionPlanChangePreparation prepareSamePlanRecovery(
      Subscription subscription,
      Payment workflowPayment
  ) {
    if (workflowPayment != null && workflowPayment.isScheduled()) {
      throw new ApplicationException(ErrorCode.ALREADY_SAME_SUBSCRIPTION_PLAN);
    }

    if (workflowPayment != null
        && !workflowPayment.isSchedulePending()
        && !workflowPayment.isScheduleUnknown()
        && !workflowPayment.isScheduleCancelPending()
        && !workflowPayment.isScheduleCancelUnknown()) {
      throw new ApplicationException(
          ErrorCode.SUBSCRIPTION_STATUS_CONFLICT,
          "기존 결제의 환불 처리가 끝난 후 결제 예약을 복구할 수 있습니다."
      );
    }

    BillingScheduleCancellationPreparation cancellation =
        BillingScheduleCancellationPreparation.notRequired(
            workflowPayment != null ? workflowPayment.getId() : null,
            subscription.getId(),
            subscription.getStatus(),
            null,
            null
        );

    if (workflowPayment != null
        && (workflowPayment.isScheduleCancelPending()
            || workflowPayment.isScheduleCancelUnknown())) {
      cancellation = BillingScheduleCancellationPreparation.cancellationRequired(
          subscription,
          workflowPayment,
          null
      );
    }

    return toPreparation(subscription, cancellation);
  }

  private SubscriptionPlanChangePreparation toPreparation(
      Subscription subscription,
      BillingScheduleCancellationPreparation cancellation
  ) {
    return new SubscriptionPlanChangePreparation(
        subscription.getId(),
        subscription.getPlan(),
        subscription.getNextBillingAt(),
        cancellation
    );
  }
}
