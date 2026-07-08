package retrivr.retrivrspring.application.service.admin.membership.pay;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentMethodRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.req.PaymentMethodCreateRequest;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res.PaymentMethodDeleteResponse;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res.PaymentMethodResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodService {

  private final OrganizationRepository organizationRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public PaymentMethodResponse createPaymentMethod(
      Long loginOrganizationId,
      PaymentMethodCreateRequest request
  ) {
    LocalDateTime now = LocalDateTime.now();
    Organization organization = getOrganization(loginOrganizationId);
    boolean firstPaymentMethod = !paymentMethodRepository.existsByOrganization(organization);
    boolean shouldUseAsDefault = firstPaymentMethod || request.isDefault();

    PaymentMethod paymentMethod = paymentMethodRepository.save(
        PaymentMethod.register(
            organization,
            request.provider(),
            request.billingKey(),
            shouldUseAsDefault,
            now
        )
    );

    if (shouldUseAsDefault) {
      changeDefaultPaymentMethod(organization, paymentMethod);
    }

    return PaymentMethodResponse.from(paymentMethod);
  }

  public List<PaymentMethodResponse> getPaymentMethods(Long loginOrganizationId) {
    Organization organization = getOrganization(loginOrganizationId);
    return paymentMethodRepository.findAllByOrganizationOrderByRegisteredAtDesc(organization)
        .stream()
        .map(PaymentMethodResponse::from)
        .toList();
  }

  public PaymentMethodResponse getPaymentMethod(
      Long loginOrganizationId,
      String paymentMethodId
  ) {
    Organization organization = getOrganization(loginOrganizationId);
    PaymentMethod paymentMethod = getPaymentMethodForOwner(organization, paymentMethodId);
    return PaymentMethodResponse.from(paymentMethod);
  }

  @Transactional
  public PaymentMethodResponse changeDefaultPaymentMethod(
      Long loginOrganizationId,
      String paymentMethodId
  ) {
    Organization organization = getOrganization(loginOrganizationId);
    PaymentMethod paymentMethod = getPaymentMethodForOwner(organization, paymentMethodId);
    paymentMethod.validateActive();
    changeDefaultPaymentMethod(organization, paymentMethod);
    return PaymentMethodResponse.from(paymentMethod);
  }

  @Transactional
  public PaymentMethodDeleteResponse deletePaymentMethod(
      Long loginOrganizationId,
      String paymentMethodId
  ) {
    LocalDateTime now = LocalDateTime.now();
    Organization organization = getOrganization(loginOrganizationId);
    PaymentMethod paymentMethod = getPaymentMethodForOwner(organization, paymentMethodId);
    validateDeletable(paymentMethod);
    subscriptionRepository.findByPaymentMethod(paymentMethod)
        .filter(subscription -> subscription.getStatus() == SubscriptionStatus.CANCELED)
        .ifPresent(Subscription::clearPaymentMethod);
    paymentMethod.disable(now);

    return new PaymentMethodDeleteResponse(
        paymentMethod.getId(),
        paymentMethod.getStatus(),
        paymentMethod.getDisabledAt()
    );
  }

  private void changeDefaultPaymentMethod(
      Organization organization,
      PaymentMethod paymentMethod
  ) {
    paymentMethodRepository.findDefaultPaymentMethods(organization)
        .forEach(PaymentMethod::unmarkDefault);
    paymentMethod.markDefault();

    subscriptionRepository.findByOrganization(organization)
        .ifPresent(subscription -> subscription.changePaymentMethod(paymentMethod));
  }

  private void validateDeletable(PaymentMethod paymentMethod) {
    subscriptionRepository.findByPaymentMethod(paymentMethod)
        .filter(subscription -> subscription.getStatus() != SubscriptionStatus.CANCELED)
        .ifPresent(subscription -> {
          throw new ApplicationException(ErrorCode.SUBSCRIPTION_STATUS_CONFLICT);
        });
  }

  private PaymentMethod getPaymentMethodForOwner(
      Organization organization,
      String paymentMethodId
  ) {
    PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_EXCEPTION));
    if (!paymentMethod.getOrganization().getId().equals(organization.getId())) {
      throw new ApplicationException(ErrorCode.FORBIDDEN_EXCEPTION);
    }
    return paymentMethod;
  }

  private Organization getOrganization(Long loginOrganizationId) {
    return organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }
}
