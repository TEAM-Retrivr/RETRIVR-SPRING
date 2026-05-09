package retrivr.retrivrspring.application.service.admin.membership.coupon;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRegistrationRepository;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.coupon.res.AdminCouponCodeCheckResponse;
import retrivr.retrivrspring.presentation.admin.coupon.res.CouponRegistrationResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponRegistrationService {

  private final CouponRepository couponRepository;
  private final CouponRegistrationRepository couponRegistrationRepository;
  private final OrganizationRepository organizationRepository;
  private final MembershipPassService membershipPassService;

  @Transactional
  public CouponRegistrationResponse registerCoupon(Long organizationId, String couponId) {
    LocalDateTime now = LocalDateTime.now();

    // 로그인 조직 검사
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    // 쿠폰 조회
    Coupon coupon = couponRepository.findByIdForUpdate(couponId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_COUPON));

    // 활성화된 쿠폰인지 검증
    if (!coupon.isActive(now)) {
      throw new ApplicationException(ErrorCode.NOT_AVAILABLE_COUPON);
    }

    // 수량이 초과되었는지 검증
    if (coupon.isQuantityExceeded()) {
      throw new ApplicationException(ErrorCode.COUPON_AVAILABLE_QUANTITY_OVERFLOW);
    }

    // 이미 등록한 쿠폰인지 확인
    if (couponRegistrationRepository.existsByOrganizationAndCoupon(organization, coupon)) {
      throw new ApplicationException(ErrorCode.ALREADY_REGISTERED_COUPON);
    }

    CouponRegistration couponRegistration = CouponRegistration.register(organization, coupon, now);

    try {
      couponRegistrationRepository.saveAndFlush(couponRegistration);
    } catch (DataIntegrityViolationException e) {
      throw new ApplicationException(ErrorCode.ALREADY_REGISTERED_COUPON);
    }

    coupon.registered(now);

    MembershipPass membershipPass = membershipPassService.generateCouponMembershipPass(
        organizationId, couponRegistration);

    return new CouponRegistrationResponse(
        organization.getId(),
        couponRegistration.getId(),
        coupon.getId(),
        membershipPass.getId()
    );
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
