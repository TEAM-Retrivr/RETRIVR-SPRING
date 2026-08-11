package retrivr.retrivrspring.infrastructure.repository.membership.coupon;

import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;

public interface CouponRegistrationRepositoryCustom {
  boolean saveIfAbsent(CouponRegistration couponRegistration);
}
