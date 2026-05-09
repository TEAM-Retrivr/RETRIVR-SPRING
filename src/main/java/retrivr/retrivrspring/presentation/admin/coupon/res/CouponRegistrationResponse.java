package retrivr.retrivrspring.presentation.admin.coupon.res;


public record CouponRegistrationResponse(
    Long organizationId,
    String couponRegistrationId,
    String couponId,
    String membershipPassId
) {

}
