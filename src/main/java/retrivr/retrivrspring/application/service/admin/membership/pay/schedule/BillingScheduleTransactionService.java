package retrivr.retrivrspring.application.service.admin.membership.pay.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCustomerRequest;

@Service
@RequiredArgsConstructor
public class BillingScheduleTransactionService {

  private static final List<PaymentStatus> REUSABLE_STATUSES = List.of(
      PaymentStatus.SCHEDULE_PENDING,
      PaymentStatus.SCHEDULE_UNKNOWN,
      PaymentStatus.SCHEDULED
  );

  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;

  @Transactional
  public BillingSchedulePreparation prepare(String subscriptionId, LocalDateTime billingAt) {
    Subscription subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    Payment existing = paymentRepository
        .findFirstByOrganizationAndStatusInOrderByCreatedAtDesc(
            subscription.getOrganization(),
            REUSABLE_STATUSES
        )
        .orElse(null);

    if (existing != null) {
      return toPreparation(subscription, existing, existing.isScheduled());
    }

    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    Payment payment = paymentRepository.saveAndFlush(
        Payment.pendingSchedule(
            "sub_schedule_" + UUID.randomUUID(),
            subscription.getPlan(),
            subscription.getOrganization(),
            (long) subscription.getPlan().getPrice(),
            paymentMethod.getProvider(),
            billingAt
        )
    );
    return toPreparation(subscription, payment, false);
  }

  @Transactional(readOnly = true)
  public BillingSchedulePreparation prepareRetry(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    return toPreparation(subscription, payment, payment.isScheduled());
  }

  @Transactional
  public void complete(String paymentId, String scheduleId, LocalDateTime billingAt) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
    if (payment.isScheduled()) {
      return;
    }
    payment.completeSchedule(scheduleId);

    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    subscription.schedulePayment(scheduleId, billingAt);
  }

  @Transactional
  public void markUnknown(String paymentId, String reason) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
    if (payment.isScheduled()) {
      return;
    }
    payment.markScheduleUnknown(reason, LocalDateTime.now());
  }

  private BillingSchedulePreparation toPreparation(
      Subscription subscription,
      Payment payment,
      boolean alreadyScheduled
  ) {
    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    Organization organization = subscription.getOrganization();
    return new BillingSchedulePreparation(
        payment.getId(),
        subscription.getId(),
        paymentMethod.getBillingKeyOrThrow(),
        paymentMethod.getProvider(),
        payment.getPlan(),
        payment.getAmount(),
        payment.getScheduledAt(),
        PortOneCustomerRequest.of(
            String.valueOf(organization.getId()),
            organization.getName(),
            organization.getEmail(),
            null
        ),
        alreadyScheduled
    );
  }
}
