package retrivr.retrivrspring.infrastructure.repository.membership.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.repository.membership.coupon.CouponRegistrationRepository;

@Repository
@RequiredArgsConstructor
public class CouponRegistrationRepositoryCustomImpl implements CouponRegistrationRepositoryCustom {

  private final CouponRegistrationRepository couponRegistrationRepository;

  @Override
  public boolean saveIfAbsent(
      CouponRegistration couponRegistration
  ) {
    try {
      couponRegistrationRepository.saveAndFlush(couponRegistration);
      return true;
    } catch (DataIntegrityViolationException e) {
      return false;
    }
  }
}
