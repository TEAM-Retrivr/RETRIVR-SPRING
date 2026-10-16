package retrivr.retrivrspring.domain.entity.locker.enumerate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LockerExtensionStatus {
  REQUESTED("요청됨"),
  APPROVED("승인됨"),
  REJECTED("거절됨");

  private final String korean;
}
