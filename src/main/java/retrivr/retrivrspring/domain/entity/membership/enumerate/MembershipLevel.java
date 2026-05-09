package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MembershipLevel {
  PREMIUM("프리미엄"),
  FREE("무료") // MembershipPass 에서 사용하지 않음
  ;

  private final String korean;
}
