package retrivr.retrivrspring.application.service.admin.membership.subscription;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pay.method.PaymentMethodService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentMethodRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.subscription.req.SubscriptionStartRequest;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionStartResponse;

@Service
@RequiredArgsConstructor
public class ScheduledSubscriptionStartService {

  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentMethodService paymentMethodService;

  @Transactional
  public SubscriptionStartResponse start(
      Long organizationId,
      String membershipPassId,
      SubscriptionStartRequest request,
      LocalDateTime now
  ) {
    Organization organization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    MembershipPass membershipPass = membershipPassRepository.findById(membershipPassId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_PASS));
    membershipPass.validateOwner(organization);
    
    LocalDateTime nextBillingAt = membershipPass.getEndAt();
    
    PaymentMethod paymentMethod = paymentMethodRepository
        .findByIdAndOrganization(request.paymentMethodId(), organization)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.INVALID_VALUE_EXCEPTION,
            "결제 수단을 찾을 수 없습니다."
        ));
    paymentMethod.validateActive();

    // Subscription 정보 생성
    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElse(null);
    if (subscription == null) {
      // 최초 생성
      subscription = Subscription.start(organization, request.plan(), now);
      subscriptionRepository.save(subscription);
    } else if (subscription.isPaused()) {
      // 재시작
      subscription.restart(
          organization,
          request.plan(),
          now,
          nextBillingAt
      );
    } else {
      throw new ApplicationException(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    }
    
    // 기본 결제수단을 구독시 결제수단으로 변경
    paymentMethodService.changeDefaultPaymentMethod(organizationId, paymentMethod.getId());
    // 다음 결제일 저장
    subscription.scheduleNextBillingAt(nextBillingAt);

    return SubscriptionStartResponse.from(subscription, membershipPass);
  }
}
