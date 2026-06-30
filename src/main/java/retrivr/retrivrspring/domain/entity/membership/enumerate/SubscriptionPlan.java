package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionPlan {
  MONTHLY("월간", 3999),
  YEARLY("연간", 39999);

  private final String korean;
  private final int price;
}
