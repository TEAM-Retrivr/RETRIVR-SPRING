package retrivr.retrivrspring.presentation.admin.locker.res.zone;

import java.util.List;

public record AdminLockerZoneListResponse(
    List<AdminLockerZoneResponse> zones
) {
}
