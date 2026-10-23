package retrivr.retrivrspring.presentation.admin.locker.req.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminLockerZoneNameUpdateRequest(
    @NotBlank(message = "구역 이름 값은 공백일 수 없습니다.") @Size(max = 20, message = "구역 이름 값은 20자 이하여야 합니다.") String name,
    @NotNull(message = "버전 값은 필수입니다.") @PositiveOrZero(message = "버전 값은 0 이상이어야 합니다.") Long version
) {

}
