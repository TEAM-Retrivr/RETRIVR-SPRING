package retrivr.retrivrspring.presentation.admin.locker.res.zone;

import java.util.UUID;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;

public record AdminLockerZoneResponse(
    UUID zoneId,
    String name,
    int sortOrder,
    int rowCount,
    int columnCount,
    int maxLockerCount,
    int availableLockerCount,
    Long version
) {

  public static AdminLockerZoneResponse from(
      LockerZone zone) {
    return new AdminLockerZoneResponse(zone.getId(), zone.getName(), zone.getSortOrder(),
        zone.getRowCount(), zone.getColumnCount(), zone.getMaxLockerCount(),
        zone.getAvailableLockerCount(), zone.getVersion());
  }
}
