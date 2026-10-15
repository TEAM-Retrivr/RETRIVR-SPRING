package retrivr.retrivrspring.presentation.admin.locker.req.zone;

import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerOperationStatus;

public enum LockerFilteringStatus {
  AVAILABLE,
  PENDING,
  RENTED,
  UNAVAILABLE
  ;

  public LockerOperationStatus toLockerOperationStatus() {
    return switch(this) {
      case AVAILABLE -> LockerOperationStatus.AVAILABLE;
      case PENDING -> LockerOperationStatus.PENDING;
      case RENTED -> LockerOperationStatus.RENTED;
      case UNAVAILABLE -> LockerOperationStatus.UNAVAILABLE;
    };
  }
}
