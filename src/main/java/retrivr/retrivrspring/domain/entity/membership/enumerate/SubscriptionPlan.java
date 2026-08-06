package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionPlan {
  MONTHLY("월간", 4900, 31),
  YEARLY("연간", 52900, 365);

  private final String korean;
  private final int price;
  private final int duration;
}
