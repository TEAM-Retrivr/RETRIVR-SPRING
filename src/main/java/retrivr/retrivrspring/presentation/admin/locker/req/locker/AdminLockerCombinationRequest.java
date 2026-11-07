package retrivr.retrivrspring.presentation.admin.locker.req.locker;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminLockerCombinationRequest(
    @NotNull(message = "병합할 대상 사물함 ID는 필수입니다.")
    UUID targetLockerId,
    @NotNull(message = "병합할 부모 사물함 ID는 필수입니다.")
    UUID parentLockerId
) {

}
