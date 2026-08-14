package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentMethodRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
public class SubscriptionStartPreparationService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentMethodRepository paymentMethodRepository;

  @Transactional
  public SubscriptionStartPreparation prepare(
      Long organizationId,
      String paymentMethodId,
      SubscriptionPlan plan,
      LocalDateTime now
  ) {
    if (plan == null) {
      throw new ApplicationException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }
    if (paymentMethodId == null || paymentMethodId.isBlank()) {
      throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION, "결제 수단이 필요합니다.");
    }

    // 이 짧은 트랜잭션 안에서만 잠금을 유지한다. PG API는 커밋 이후 호출한다.
    Organization organization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    PaymentMethod paymentMethod = paymentMethodRepository
        .findByIdAndOrganization(paymentMethodId, organization)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.INVALID_VALUE_EXCEPTION,
            "결제 수단을 찾을 수 없습니다."
        ));

    paymentMethod.validateActive();

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElse(null);

    if (subscription == null) {
      subscription = subscriptionRepository.save(
          Subscription.prepareStart(organization, plan, paymentMethod, now)
      );
    } else if (subscription.isStartPending()) {
      throw new ApplicationException(ErrorCode.PAYMENT_CONFIRMATION_PENDING);
    } else if (subscription.isActive()) {
      throw new ApplicationException(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    } else {
      subscription.prepareRestart(organization, plan, paymentMethod);
    }

    return new SubscriptionStartPreparation(
        organization.getId(),
        subscription.getId(),
        paymentMethod.getId(),
        plan
    );
  }
}
