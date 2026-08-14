package retrivr.retrivrspring.application.service.admin.membership.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
public class SubscriptionStartCompletionService {

  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final MembershipPassService membershipPassService;

  @Transactional
  public SubscriptionStartResponse completeImmediatePayment(
      String paymentId
  ) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));

    if (!payment.isSuccess()) {
      throw new ApplicationException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));

    subscription.validateOwner(payment.getOrganization());

    // 동일 결제의 완료 처리가 재시도된 경우 패스를 다시 생성하지 않는다.
    MembershipPass existingPass = membershipPassRepository.findByPayment(payment).orElse(null);
    if (existingPass != null) {
      return SubscriptionStartResponse.from(subscription, existingPass);
    }

    if (!subscription.isStartPending()) {
      throw new ApplicationException(ErrorCode.SUBSCRIPTION_STATUS_CONFLICT);
    }

    subscription.completeSuccessfulPayment(payment.getPaidAt());

    MembershipPass membershipPass =
        membershipPassService.generateSubscriptionMembershipPassWithPayment(
            payment.getOrganization().getId(),
            payment,
            subscription
        );

    subscription.scheduleNextBillingAt(membershipPass.getEndAt());
    return SubscriptionStartResponse.from(subscription, membershipPass);
  }

  @Transactional
  public void failImmediatePayment(
      String paymentId
  ) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));

    if (!payment.isFailed()) {
      throw new ApplicationException(ErrorCode.PAYMENT_STATUS_TRANSITION_EXCEPTION);
    }

    Subscription subscription = subscriptionRepository.findByOrganization(payment.getOrganization())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_SUBSCRIPTION));

    subscription.validateOwner(payment.getOrganization());
    if (subscription.isPaymentFailed()) {
      return;
    }
    subscription.failStart(payment.getFailedAt());
  }

  @Transactional
  public void requireCompensation(String paymentId, String reason) {
    Payment payment = paymentRepository.findWithLockById(paymentId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.DO_NOT_GET_PAYMENT));

    // 동일 보상 요청의 재처리를 허용한다.
    if (payment.isCompensationRequired()) {
      return;
    }

    payment.requireCompensation(reason);
  }
}
