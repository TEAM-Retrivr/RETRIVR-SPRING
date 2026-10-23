package retrivr.retrivrspring.presentation.admin.locker.req.locker;

import jakarta.validation.constraints.*;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerEnableStatus;

public record AdminLockerEnableRequest(
    @NotNull(message = "활성화 상태 값은 필수입니다.") LockerEnableStatus enableStatus
) {

}
