package retrivr.retrivrspring.domain.entity.locker.enumerate;

/** 관리자 목록에서 사용하는 상태. 비활성 사물함은 UNAVAILABLE로 표시한다. */
public enum LockerStatus {
  PENDING, AVAILABLE, RENTED, UNAVAILABLE, DISABLED;

  public static LockerStatus from(LockerEnableStatus enableStatus, LockerOperationStatus operation) {
    return enableStatus == LockerEnableStatus.DISABLED ? DISABLED : valueOf(operation.name());
  }
}
