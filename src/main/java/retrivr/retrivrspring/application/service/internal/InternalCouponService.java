package retrivr.retrivrspring.application.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.port.id.PublicIdGenerator;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.internal.req.InternalCouponCreateRequest;
import retrivr.retrivrspring.presentation.internal.res.InternalCouponCreateResponse;

@Service
@RequiredArgsConstructor
public class InternalCouponService {

  private final CouponRepository couponRepository;
  private final PublicIdGenerator publicIdGenerator;

  private static final int MAX_CODE_GENERATE_RETRY = 5;

  @Transactional
  public InternalCouponCreateResponse createCoupon(InternalCouponCreateRequest request) {
    String code = "";

    for (int i = 0; i < MAX_CODE_GENERATE_RETRY; i++) {
      code = publicIdGenerator.generateCouponCode();
      if (!couponRepository.existsByCode(code)) {
        break;
      }
    }

    Coupon coupon = Coupon.create(
        code,
        request.name(),
        request.guideline(),
        request.description(),
        request.totalQuantity(),
        request.durationDays(),
        request.activeStartAt(),
        request.expiresAt()
    );

    try {
      Coupon savedCoupon = couponRepository.saveAndFlush(coupon);
      return InternalCouponCreateResponse.from(savedCoupon);
    } catch (DataIntegrityViolationException ignored) {
      throw new ApplicationException(ErrorCode.COUPON_CODE_GENERATE_FAILED);
    }
  }
}
