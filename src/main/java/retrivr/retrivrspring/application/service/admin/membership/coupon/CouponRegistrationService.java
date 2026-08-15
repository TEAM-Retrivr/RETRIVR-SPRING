package retrivr.retrivrspring.application.service.admin.membership.coupon;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleCancellationService;
import retrivr.retrivrspring.application.service.admin.membership.pay.schedule.BillingScheduleRequestService;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRegistrationRepository;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.coupon.res.AdminCouponCodeCheckResponse;
import retrivr.retrivrspring.presentation.admin.membership.coupon.res.CouponRegistrationResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponRegistrationService {

  private final CouponRepository couponRepository;
  private final CouponRegistrationRepository couponRegistrationRepository;
  private final OrganizationRepository organizationRepository;
  private final CouponRegistrationTransactionService transactionService;
  private final BillingScheduleCancellationService cancellationService;
  private final BillingScheduleRequestService requestService;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public CouponRegistrationResponse registerCoupon(Long organizationId, String couponId) {
    // 쿠폰 등록 준비
    // 쿠폰 상태, Payment 상태 등 변경
    CouponRegistrationPreparation preparation = transactionService.prepare(
        organizationId,
        couponId,
        LocalDateTime.now()
    );
    
    // 실제 결제 예약 취소
    if (preparation.billingScheduleChangeRequired()) {
      boolean previousScheduleCanceled = cancellationService.cancel(
          preparation.cancellation()
      );
      if (previousScheduleCanceled) {
        requestBillingScheduleSafely(preparation);
      }
    }

    return new CouponRegistrationResponse(
        preparation.organizationId(),
        preparation.couponRegistrationId(),
        preparation.couponId(),
        preparation.membershipPassId()
    );
  }

  private void requestBillingScheduleSafely(CouponRegistrationPreparation preparation) {
    try {
      requestService.request(preparation.subscriptionId());
    } catch (RuntimeException exception) {
      log.error(
          "쿠폰 등록 후 결제 예약 요청 실패. subscriptionId={}",
          preparation.subscriptionId(),
          exception
      );
    }
  }

  public AdminCouponCodeCheckResponse checkCouponCode(Long organizationId, String couponCode) {
    LocalDateTime now = LocalDateTime.now();
    // 로그인 조직 검사
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    // 쿠폰 조회
    String upperCouponCode = couponCode.toUpperCase();
    Optional<Coupon> couponOpt = couponRepository.findByCode(upperCouponCode);

    // 조회되지 않을 경우
    if (couponOpt.isEmpty()) {
      return AdminCouponCodeCheckResponse.empty();
    }
    Coupon coupon = couponOpt.get();

    // 만료되었거나 비활성화 상태일 경우 혹은 유효기간 시작이 되지 않았을 경우
    if (!coupon.isActive(now)) {
      return AdminCouponCodeCheckResponse.empty();
    }

    // 이미 사용한 쿠폰인지 확인
    Optional<CouponRegistration> byOrganizationAndCoupon = couponRegistrationRepository.findByOrganizationAndCoupon(
        organization, coupon);

    boolean isUsed = byOrganizationAndCoupon.isPresent();

    return AdminCouponCodeCheckResponse.of(
        true,
        isUsed,
        coupon
    );
  }

}
