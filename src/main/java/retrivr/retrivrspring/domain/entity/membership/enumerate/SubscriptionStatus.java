package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionStatus {
  ACTIVE("활성화됨"),
  CANCELED("취소됨"),
  PAST_DUE("결제 재시도 중"),
  PAYMENT_FAILED("결제 실패");

  private final String korean;
}
