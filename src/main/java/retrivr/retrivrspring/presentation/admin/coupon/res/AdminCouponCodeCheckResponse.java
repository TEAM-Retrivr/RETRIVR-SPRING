package retrivr.retrivrspring.presentation.admin.coupon.res;

import java.time.LocalDate;
import retrivr.retrivrspring.domain.entity.membership.Coupon;

public record AdminCouponCodeCheckResponse(
    boolean isExist,
    boolean isUsed,
    String couponId,
    String name,
    String description,
    int durationDays,
    LocalDate activeStartDay,
    LocalDate expiresDay,
    String guideline
) {
  public static AdminCouponCodeCheckResponse empty() {
    return new AdminCouponCodeCheckResponse(false, false, null, null, null, 0, null, null, null);
  }

  public static AdminCouponCodeCheckResponse of(boolean isExist, boolean isUsed, Coupon coupon) {
    return new AdminCouponCodeCheckResponse(
        isExist,
        isUsed,
        coupon.getId(),
        coupon.getName(),
        coupon.getDescription(),
        coupon.getDurationDays(),
        LocalDate.from(coupon.getActiveStartAt()),
        LocalDate.from(coupon.getExpiresAt()),
        coupon.getGuideline()
    );
  }
}
