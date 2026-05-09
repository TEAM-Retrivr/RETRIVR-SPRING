package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipPassType {
  SUBSCRIPTION("구독형"),
  COUPON("쿠폰형");

  private final String korean;
}
