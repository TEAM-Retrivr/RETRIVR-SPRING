package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipPassStatus {
  REGISTERED("등록됨"),
  ACTIVE("사용중"),
  EXPIRED("만료됨");

  private final String korean;
}
