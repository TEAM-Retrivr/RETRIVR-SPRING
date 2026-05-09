package retrivr.retrivrspring.presentation.internal.res;

import java.time.LocalDate;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.enumerate.CouponStatus;

public record InternalCouponCreateResponse(
    String couponId,
    String code,
    String name,
    int durationDays,
    int totalQuantity,
    int usedQuantity,
    LocalDate expiresAt,
    CouponStatus status,
    LocalDateTime createdAt
) {

  public static InternalCouponCreateResponse from(Coupon coupon) {
    return new InternalCouponCreateResponse(
        coupon.getId(),
        coupon.getCode(),
        coupon.getName(),
        coupon.getDurationDays(),
        coupon.getTotalQuantity(),
        coupon.getUsedQuantity(),
        coupon.getExpiresAt(),
        coupon.getStatus(),
        coupon.getCreatedAt()
    );
  }
}