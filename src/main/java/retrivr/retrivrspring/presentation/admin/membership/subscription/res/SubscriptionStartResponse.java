package retrivr.retrivrspring.presentation.admin.membership.subscription.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;

public record SubscriptionStartResponse(
    String subscriptionId,
    SubscriptionPlan plan,
    SubscriptionStatus status,
    LocalDateTime nextBillingAt,
    String membershipPassId,
    LocalDateTime startAt,
    LocalDateTime expireAt
) {

  public static SubscriptionStartResponse from(
      Subscription subscription,
      MembershipPass membershipPass
  ) {
    return new SubscriptionStartResponse(
        subscription.getId(),
        subscription.getPlan(),
        subscription.getStatus(),
        subscription.getNextBillingAt(),
        membershipPass.getId(),
        membershipPass.getStartAt(),
        membershipPass.getEndAt()
    );
  }
}
