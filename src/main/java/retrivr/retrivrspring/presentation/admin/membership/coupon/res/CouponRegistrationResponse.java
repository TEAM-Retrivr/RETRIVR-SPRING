package retrivr.retrivrspring.presentation.admin.membership.coupon.res;


public record CouponRegistrationResponse(
    Long organizationId,
    String couponRegistrationId,
    String couponId,
    String membershipPassId
) {

}
