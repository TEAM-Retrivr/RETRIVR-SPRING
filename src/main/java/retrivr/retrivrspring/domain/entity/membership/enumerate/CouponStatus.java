package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponStatus {
  ACTIVE("사용 가능"),
  DISABLED("비활성화"),
  EXPIRED("쿠폰 만료");

  private final String korean;
}
