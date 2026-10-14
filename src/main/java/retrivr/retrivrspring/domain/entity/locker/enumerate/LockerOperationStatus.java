package retrivr.retrivrspring.domain.entity.locker.enumerate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LockerOperationStatus {
  AVAILABLE("대여 가능"),
  PENDING("승인 대기"),
  RENTED("대여 중")
  ;

  private final String korean;
}

