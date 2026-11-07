package retrivr.retrivrspring.presentation.admin.locker.res.locker;

import java.util.UUID;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.enumerate.*;

public record AdminLockerResponse(
    UUID lockerId,
    String label,
    int rowIndex,
    int columnIndex,
    UUID parent,
    LockerEnableStatus enableStatus,
    LockerOperationStatus operationStatus,
    Long version
) {

  public static AdminLockerResponse from(Locker locker) {
    return new AdminLockerResponse(
        locker.getId(),
        locker.getLabel(),
        locker.getRowIndex(),
        locker.getColumnIndex(),
        locker.getParentLocker() == null ? null : locker.getParentLocker().getId(),
        locker.getEnableStatus(),
        locker.getOperationStatus(),
        locker.getVersion()
    );
  }
}
