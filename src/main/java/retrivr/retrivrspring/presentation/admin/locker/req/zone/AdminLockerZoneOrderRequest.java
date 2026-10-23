package retrivr.retrivrspring.presentation.admin.locker.req.zone;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record AdminLockerZoneOrderRequest(
    @NotNull(message = "구역 ID 목록 값은 필수입니다.") List<@NotNull(message = "구역 ID 목록에 null을 포함할 수 없습니다.") UUID> zoneIds
) {
}
