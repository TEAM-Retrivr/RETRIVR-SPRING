package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneClient;
import retrivr.retrivrspring.infrastructure.payment.portone.PortOneException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCustomerRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentAmountRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortOnePaymentService {

  private static final String CURRENCY_KRW = "KRW";

  private final PortOneClient portOneClient;
  private final PaymentRepository paymentRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment charge(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      LocalDateTime now
  ) {
    String paymentId = createPaymentId(subscription, "instant");
    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    try {
      PortOneBillingKeyPaymentResponse response = portOneClient.chargeBillingKey(
          new PortOneBillingKeyPaymentRequest(
              paymentId,
              null,
              paymentMethod.getBillingKeyOrThrow(),
              null,
              orderName(plan),
              customer(organization),
              null,
              PortOnePaymentAmountRequest.total(plan.getPrice()),
              CURRENCY_KRW,
              null,
              null,
              1,
              false
          )
      );

      PortOnePaymentResponse portOnePayment = verifyPaidPayment(paymentId, plan.getPrice());
      PaymentProvider provider = resolveProvider(portOnePayment);
      LocalDateTime paidAt = resolvePaidAt(response, portOnePayment, now);

      Payment payment = Payment.success(
          paymentId,
          subscription.getPlan(),
          organization,
          portOnePayment.scheduleId(),
          (long) plan.getPrice(),
          provider,
          providerPaymentKey(response),
          paidAt
      );
      return paymentRepository.save(payment);
    } catch (PortOneException | ApplicationException e) {
      Payment payment = Payment.fail(
          paymentId,
          subscription.getPlan(),
          organization,
          (long) plan.getPrice(),
          paymentMethod.getProvider(),
          "PORTONE_PAYMENT_FAILED",
          e.getMessage(),
          now
      );
      return paymentRepository.save(payment);
    }
  }

  @Transactional
  public Payment fail(
      String paymentId,
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    Payment payment = Payment.fail(
        paymentId,
        subscription.getPlan(),
        organization,
        (long) plan.getPrice(),
        resolvePaymentMethodProvider(subscription),
        failureCode,
        failureReason,
        failedAt
    );
    return paymentRepository.save(payment);
  }

  @Transactional
  public PortOneScheduleBillingPaymentResponse scheduleBillingPayment(
      Subscription subscription,
      LocalDateTime timeToPay
  ) {
    PaymentMethod paymentMethod = subscription.getPaymentMethodOrThrow();
    String paymentId = createPaymentId(subscription, "schedule");
    PortOneScheduleBillingPaymentRequest request = new PortOneScheduleBillingPaymentRequest(
        paymentId,
        null,
        paymentMethod.getBillingKeyOrThrow(),
        null,
        orderName(subscription.getPlan()),
        customer(subscription.getOrganization()),
        null,
        PortOnePaymentAmountRequest.total(subscription.getPlan().getPrice()),
        CURRENCY_KRW,
        null,
        null,
        1,
        toOffsetDateTime(timeToPay)
    );

    PortOneScheduleBillingPaymentResponse response = portOneClient.scheduleBillingPayment(request);
    if (response == null || response.schedule() == null || response.schedule().id() == null) {
      throw new ApplicationException(ErrorCode.PAYMENT_RESERVATION_FAILED);
    }

    subscription.schedulePayment(response.schedule().id(), timeToPay);
    Payment payment = Payment.schedule(
        paymentId,
        subscription.getPlan(),
        subscription.getOrganization(),
        response.schedule().id(),
        (long) subscription.getPlan().getPrice(),
        paymentMethod.getProvider(),
        timeToPay
    );
    paymentRepository.save(payment);

    return response;
  }

  @Transactional
  public PortOneCancelScheduledPaymentResponse cancelScheduledPayment(Payment payment) {
    if (payment.getPortOneScheduleId() == null || payment.getPortOneScheduleId().isBlank()) {
      return new PortOneCancelScheduledPaymentResponse(List.of(), null);
    }

    PortOneCancelScheduledPaymentResponse response = portOneClient.cancelScheduledPayment(
        PortOneCancelScheduledPaymentRequest.byScheduleId(payment.getPortOneScheduleId())
    );
    payment.scheduledCancel(response.revokedAt().toLocalDateTime());

    return response;
  }

  public PortOnePaymentResponse verifyPaidPayment(String paymentId, long expectedAmount) {
    PortOnePaymentResponse payment = portOneClient.getPayment(paymentId);
    if (payment == null || !payment.hasPaymentId(paymentId)) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }
    if (!payment.hasTotalAmount(expectedAmount)) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }
    if (!payment.isPaid()) {
      throw new ApplicationException(ErrorCode.PAYMENT_FAILED);
    }
    return payment;
  }

  private String createPaymentId(Subscription subscription, String reason) {
    return "sub_" + subscription.getId() + "_" + reason + "_" + UUID.randomUUID();
  }

  private String orderName(SubscriptionPlan plan) {
    return "Retrivr " + plan.getKorean() + " 구독";
  }

  private PortOneCustomerRequest customer(Organization organization) {
    return PortOneCustomerRequest.of(
        String.valueOf(organization.getId()),
        organization.getName(),
        organization.getEmail(),
        null
    );
  }

  private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
    return time.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }

  private LocalDateTime resolvePaidAt(
      PortOneBillingKeyPaymentResponse response,
      PortOnePaymentResponse payment,
      LocalDateTime fallback
  ) {
    if (response != null && response.payment() != null && response.payment().paidAt() != null) {
      return response.payment().paidAt().toLocalDateTime();
    }
    if (payment != null && payment.paidAt() != null) {
      return payment.paidAt().toLocalDateTime();
    }
    return fallback;
  }

  private String providerPaymentKey(PortOneBillingKeyPaymentResponse response) {
    if (response == null || response.payment() == null) {
      return null;
    }
    return response.payment().pgTxId();
  }

  private PaymentProvider resolveProvider(PortOnePaymentResponse payment) {
    if (payment != null && payment.channel() != null && payment.channel().pgProvider() != null) {
      String pgProvider = payment.channel().pgProvider();
      if (pgProvider.contains("TOSS")) {
        return PaymentProvider.TOSS;
      }
      if (pgProvider.contains("KAKAO")) {
        return PaymentProvider.KAKAOPAY;
      }
    }
    return PaymentProvider.KAKAOPAY;
  }

  private PaymentProvider resolvePaymentMethodProvider(Subscription subscription) {
    return subscription.getPaymentMethodOrThrow().getProvider();
  }
}
