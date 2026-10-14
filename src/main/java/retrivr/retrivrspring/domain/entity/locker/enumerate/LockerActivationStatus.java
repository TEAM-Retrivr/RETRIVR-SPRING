package retrivr.retrivrspring.domain.entity.locker.enumerate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LockerActivationStatus {
  ENABLED("활성화"),
  DISABLED("비활성화")
  ;

  private final String korean;
}
