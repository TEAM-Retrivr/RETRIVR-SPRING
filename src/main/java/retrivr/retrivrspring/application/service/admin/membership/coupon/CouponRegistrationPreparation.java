package retrivr.retrivrspring.application.service.admin.membership.coupon;

import java.time.LocalDateTime;
import retrivr.retrivrspring.application.service.admin.membership.subscription.BillingScheduleCancellationPreparation;

public record CouponRegistrationPreparation(
    Long organizationId,
    String couponRegistrationId,
    String couponId,
    String membershipPassId,
    String subscriptionId,
    LocalDateTime nextBillingAt,
    BillingScheduleCancellationPreparation cancellation
) {

  public boolean billingScheduleChangeRequired() {
    return subscriptionId != null && nextBillingAt != null;
  }
}
