package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionPlan {
  MONTHLY("월간"),
  YEARLY("연간");

  private final String korean;
}
