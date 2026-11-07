package retrivr.retrivrspring.presentation.admin.locker.res.locker;

import java.util.UUID;

public record AdminLockerMutationResponse(
    UUID zoneId,
    Long zoneVersion,
    AdminLockerResponse locker
) {

}
