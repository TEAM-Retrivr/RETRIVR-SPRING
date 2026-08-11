package retrivr.retrivrspring.infrastructure.repository.membership.coupon;

import java.time.LocalDate;
import retrivr.retrivrspring.domain.entity.membership.Coupon;

public interface CouponRepositoryCustom {

  boolean consumeIfAvailable(Coupon coupon, LocalDate today);
}
