package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockPaymentService implements PaymentService {

  private final PaymentRepository paymentRepository;

  @Override
  @Transactional
  public Payment manualPayment(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      LocalDateTime now
  ) {
    Payment payment = Payment.success(
        organization,
        subscription,
        plan,
        PaymentProvider.MOCK,
        now
    );
    return paymentRepository.save(payment);
  }

  @Override
  @Transactional
  public Payment autoPayment(Subscription subscription, LocalDateTime now) {
    Payment payment = Payment.success(
        subscription.getOrganization(),
        subscription,
        subscription.getPlan(),
        PaymentProvider.MOCK,
        now
    );
    return paymentRepository.save(payment);
  }

  @Override
  @Transactional
  public Payment fail(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    Payment payment = Payment.fail(
        organization,
        subscription,
        plan,
        PaymentProvider.MOCK,
        failureCode,
        failureReason,
        failedAt
    );
    return paymentRepository.save(payment);
  }
}
