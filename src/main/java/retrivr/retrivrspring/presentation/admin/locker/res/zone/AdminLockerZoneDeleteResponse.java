package retrivr.retrivrspring.presentation.admin.locker.res.zone;

import java.util.UUID;

public record AdminLockerZoneDeleteResponse(
    UUID zoneId,
    boolean deleted
) {

}
