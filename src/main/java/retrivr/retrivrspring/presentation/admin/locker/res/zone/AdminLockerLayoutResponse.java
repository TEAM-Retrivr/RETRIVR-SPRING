package retrivr.retrivrspring.presentation.admin.locker.res.zone;

import java.util.List;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerResponse;

public record AdminLockerLayoutResponse(
    AdminLockerZoneResponse zone,
    List<AdminLockerResponse> lockers
) {

}
