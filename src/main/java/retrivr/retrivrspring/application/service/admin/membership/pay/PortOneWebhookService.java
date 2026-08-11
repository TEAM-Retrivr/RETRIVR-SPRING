package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.event.ScheduledPaymentFailEvent;
import retrivr.retrivrspring.application.event.ScheduledPaymentReconcileRequestedEvent;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassExpirationService;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionService;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortOneWebhookService {

  private final PaymentRepository paymentRepository;
  private final PortOnePaymentService paymentService;
  private final MembershipPassExpirationService membershipPassExpirationService;
  private final SubscriptionService subscriptionService;

  @EventListener
  public void handleScheduledPaymentReconcileRequested(
      ScheduledPaymentReconcileRequestedEvent event
  ) {
    handleScheduledPaymentSuccess(event.paymentId());
  }

  @EventListener
  public void handleScheduledPaymentFail(
      ScheduledPaymentFailEvent event
  ) {
    handleSchedulePaymentFail(event.paymentId());
  }

  // 자동결제 완료 시
  @Transactional
  public void handleScheduledPaymentSuccess(String paymentId) {
    LocalDateTime now = LocalDateTime.now();

    // 스케쥴러에 의해 작업이 이루어졌는지 체크
    Optional<Payment> opPayment = paymentRepository.findByIdAndStatus(paymentId,
        PaymentStatus.SCHEDULED);

    if (opPayment.isEmpty()) {
      return;
    }
    Payment payment = opPayment.get();

    // 실제로 결제가 이루어졌는지 체크
    PortOnePaymentResponse portOnePaymentResponse = paymentService.verifyPaidPayment(paymentId,
        payment.getAmount());

    if (portOnePaymentResponse.isFailed()) {
      handleSchedulePaymentFail(paymentId);
      return;
    }

    // Payment 히스토리에 기록
    payment.scheduledPaymentSuccess(
        portOnePaymentResponse.channel().resolveProvider(),
        portOnePaymentResponse.transactionId(),
        portOnePaymentResponse.paidAt().toLocalDateTime()
    );

    // 기존 멤버십 패스 만료 후 새로운 패스 생성
    Subscription subscription = membershipPassExpirationService.processExpireCurrentPassWhenPaymentSuccess(
        payment.getOrganization(),
        payment,
        now
    );

    paymentService.scheduleBillingPayment(subscription, subscription.getNextBillingAt());
  }

  @Transactional
  public void handleSchedulePaymentFail(String paymentId) {
    LocalDateTime now = LocalDateTime.now();
    Optional<Payment> opPayment = paymentRepository.findByIdAndStatus(paymentId,
        PaymentStatus.SCHEDULED);

    if (opPayment.isEmpty()) {
      return;
    }
    Payment payment = opPayment.get();

    // 실제로 결제가 이루어졌는지 체크
    PortOnePaymentResponse portOnePaymentResponse = paymentService.verifyPaidPayment(paymentId,
        payment.getAmount());

    if (!portOnePaymentResponse.isFailed()) {
      return;
    }

    payment.scheduledPaymentFail(
        portOnePaymentResponse.channel().resolveProvider(),
        portOnePaymentResponse.failure().pgCode(),
        portOnePaymentResponse.failure().reason(),
        portOnePaymentResponse.failedAt().toLocalDateTime()
    );

    membershipPassExpirationService.processExpiredPass(payment.getOrganization().getId(), now);

    subscriptionService.cancelSubscriptionWhenPaymentFail(payment.getOrganization(), now);
  }
}
