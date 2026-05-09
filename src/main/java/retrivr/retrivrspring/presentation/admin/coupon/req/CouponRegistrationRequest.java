package retrivr.retrivrspring.presentation.admin.coupon.req;

import io.swagger.v3.oas.annotations.media.Schema;

public record CouponRegistrationRequest(
    @Schema(description = "쿠폰 코드", example = "RETRIVR-30DAYS")
    String couponCode
) {

}