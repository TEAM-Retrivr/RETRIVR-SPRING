package retrivr.retrivrspring.application.service.admin.membership.coupon;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassExpirationService;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.application.service.admin.membership.subscription.BillingScheduleCancellationPreparation;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRegistrationRepository;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.membership.coupon.CouponRegistrationRepositoryCustom;

@Service
@RequiredArgsConstructor
public class CouponRegistrationTransactionService {

  private static final List<PaymentStatus> BILLING_WORKFLOW_STATUSES = List.of(
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

  private final CouponRepository couponRepository;
  private final CouponRegistrationRepositoryCustom couponRegistrationRepositoryCustom;
  private final CouponRegistrationRepository couponRegistrationRepository;
  private final OrganizationRepository organizationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final MembershipPassService membershipPassService;
  private final MembershipPassExpirationService membershipPassExpirationService;

  @Transactional
  public CouponRegistrationPreparation prepare(
      Long organizationId,
      String couponId,
      LocalDateTime now
  ) {
    Organization organization = organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_COUPON));

    validateCoupon(coupon, organization, now);

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .filter(Subscription::isActive)
        .orElse(null);
    
    // 이미 결제 예약 혹은 결제 관련 로직이 처리 중인 경우 예외 처리
    // 정확히 Scheduled 인 상태일 경우만 반환
    Payment scheduledPayment = findScheduledPaymentOrValidateWorkflow(organization, subscription);

    // DB 단에서 무결성을 체크하며 쿠폰 등록
    // 중복 등록 방지 및 쿠폰 최대 갯수 이상의 등록 방지
    CouponRegistration couponRegistration = CouponRegistration.register(organization, coupon, now);
    if (!couponRegistrationRepositoryCustom.saveIfAbsent(couponRegistration)) {
      throw new ApplicationException(ErrorCode.ALREADY_REGISTERED_COUPON);
    }
    if (!couponRepository.consumeIfAvailable(coupon, now.toLocalDate())) {
      throw new ApplicationException(ErrorCode.COUPON_AVAILABLE_QUANTITY_OVERFLOW);
    }

    // ACTIVE 지만 만료된 패스가 있다면 만료시킨다
    membershipPassExpirationService.processExpiredPassWithoutPaymentReconciliation(
        organizationId,
        now
    );

    // 바깥 트랜잭션에서 획득한 Organization Lock을 유지한 상태로 쿠폰 패스를 생성한다.
    MembershipPass membershipPass = membershipPassService.generateCouponMembershipPass(
        organizationId,
        couponRegistration
    );

    BillingScheduleCancellationPreparation cancellation =
        BillingScheduleCancellationPreparation.notRequired(null, null, null, null, null);
    String subscriptionId = null;
    LocalDateTime nextBillingAt = null;

    if (subscription != null) {
      subscriptionId = subscription.getId();
      nextBillingAt = membershipPass.getEndAt();
      subscription.rescheduleNextBillingAt(nextBillingAt);

      if (scheduledPayment != null) {
        scheduledPayment.requestScheduleCancellation(now);
        cancellation = BillingScheduleCancellationPreparation.cancellationRequired(
            subscription,
            scheduledPayment,
            membershipPass.getEndAt()
        );
      }
    }

    return new CouponRegistrationPreparation(
        organizationId,
        couponRegistration.getId(),
        coupon.getId(),
        membershipPass.getId(),
        subscriptionId,
        nextBillingAt,
        cancellation
    );
  }

  private Payment findScheduledPaymentOrValidateWorkflow(
      Organization organization,
      Subscription subscription
  ) {
    if (subscription == null) {
      return null;
    }

    Payment workflowPayment = paymentRepository
        .findFirstByOrganizationAndStatusInOrderByCreatedAtDesc(
            organization,
            BILLING_WORKFLOW_STATUSES
        )
        .orElse(null);

    if (workflowPayment != null && !workflowPayment.isScheduled()) {
      throw new ApplicationException(
          ErrorCode.SUBSCRIPTION_STATUS_CONFLICT,
          "기존 결제 예약 또는 환불 처리가 끝난 후 쿠폰을 등록할 수 있습니다."
      );
    }
    return workflowPayment;
  }

  private void validateCoupon(Coupon coupon, Organization organization, LocalDateTime now) {
    if (!coupon.isActive(now)) {
      throw new ApplicationException(ErrorCode.NOT_AVAILABLE_COUPON);
    }
    if (coupon.isQuantityExceeded()) {
      throw new ApplicationException(ErrorCode.COUPON_AVAILABLE_QUANTITY_OVERFLOW);
    }
    if (couponRegistrationRepository.existsByOrganizationAndCoupon(organization, coupon)) {
      throw new ApplicationException(ErrorCode.ALREADY_REGISTERED_COUPON);
    }
  }
}
