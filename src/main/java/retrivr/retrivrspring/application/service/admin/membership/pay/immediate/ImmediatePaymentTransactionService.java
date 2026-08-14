package retrivr.retrivrspring.application.service.admin.membership.pay.immediate;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCustomerRequest;

@Service
@RequiredArgsConstructor
public class ImmediatePaymentTransactionService {

  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;

  @Transactional
  public ImmediatePaymentPreparation prepare(String subscriptionId) {
    Subscription subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    Organization organization = subscription.getOrganization();
    String paymentId = "sub_instant_" + UUID.randomUUID();

    paymentRepository.saveAndFlush(
        Payment.pending(
            paymentId,
            subscription.getPlan(),
            organization,
            (long) subscription.getPlan().getPrice(),
            paymentMethod.getProvider()
        )
    );

    return new ImmediatePaymentPreparation(
        paymentId,
        paymentMethod.getBillingKeyOrThrow(),
        paymentMethod.getProvider(),
        subscription.getPlan(),
        subscription.getPlan().getPrice(),
        PortOneCustomerRequest.of(
            String.valueOf(organization.getId()),
            organization.getName(),
            organization.getEmail(),
            null
        )
    );
  }

  @Transactional(readOnly = true)
  public ImmediatePaymentPreparation prepareRetry(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
    if (!payment.isPending()) {
      throw new ApplicationException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    Organization organization = subscription.getOrganization();
    return new ImmediatePaymentPreparation(
        payment.getId(),
        paymentMethod.getBillingKeyOrThrow(),
        paymentMethod.getProvider(),
        payment.getPlan(),
        payment.getAmount(),
        PortOneCustomerRequest.of(
            String.valueOf(organization.getId()),
            organization.getName(),
            organization.getEmail(),
            null
        )
    );
  }

  @Transactional
  public Payment completeSuccess(
      String paymentId,
      String portOneScheduleId,
      String providerPaymentKey,
      LocalDateTime paidAt
  ) {
    Payment payment = getPendingPayment(paymentId);
    payment.completeImmediatePayment(portOneScheduleId, providerPaymentKey, paidAt);
    return payment;
  }

  @Transactional
  public Payment completeFailure(
      String paymentId,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    Payment payment = getPendingPayment(paymentId);
    payment.failPendingPayment(failureCode, failureReason, failedAt);
    failPendingSubscription(payment, failedAt);
    return payment;
  }

  @Transactional
  public Payment markUnknown(String paymentId, String reason, LocalDateTime occurredAt) {
    Payment payment = getPendingPayment(paymentId);
    payment.markPaymentUnknown(reason, occurredAt);
    return payment;
  }

  private Payment getPendingPayment(String paymentId) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));
    if (!payment.isPending()) {
      throw new ApplicationException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }
    return payment;
  }

  private void failPendingSubscription(Payment payment, LocalDateTime failedAt) {
    Subscription subscription = subscriptionRepository
        .findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));
    if (subscription.isStartPending()) {
      subscription.failStart(failedAt);
    }
  }
}
