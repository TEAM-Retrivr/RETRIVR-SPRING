package retrivr.retrivrspring.presentation.admin.locker.req.zone;

import jakarta.validation.constraints.*;

public record AdminLockerZoneLayoutUpdateRequest(
    @NotNull(message = "행 개수 값은 필수입니다.") @Min(value = 1, message = "행 개수 값은 1 이상이어야 합니다.") @Max(value = 30, message = "행 개수 값은 30 이하이어야 합니다.") Integer rowCount,
    @NotNull(message = "열 개수 값은 필수입니다.") @Min(value = 1, message = "열 개수 값은 1 이상이어야 합니다.") @Max(value = 300, message = "열 개수 값은 300 이하이어야 합니다.") Integer columnCount,
    @NotNull(message = "버전 값은 필수입니다.") @PositiveOrZero(message = "버전 값은 0 이상이어야 합니다.") Long version
) {

}
