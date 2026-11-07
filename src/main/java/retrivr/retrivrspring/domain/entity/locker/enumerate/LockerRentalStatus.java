package retrivr.retrivrspring.domain.entity.locker.enumerate;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LockerRentalStatus {
  REQUESTED("요청됨"),
  APPROVED("승인됨"),
  REJECTED("거절됨"),
  RETURNED("반납됨");

  private final String korean;

  public static final List<LockerRentalStatus> ACTIVE_RENTALS =
      List.of(LockerRentalStatus.REQUESTED, LockerRentalStatus.APPROVED);
}
