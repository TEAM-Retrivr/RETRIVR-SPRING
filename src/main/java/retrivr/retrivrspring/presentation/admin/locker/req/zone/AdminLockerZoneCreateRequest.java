package retrivr.retrivrspring.presentation.admin.locker.req.zone;

import jakarta.validation.constraints.*;

public record AdminLockerZoneCreateRequest(
    @NotBlank(message = "name 이 공백일 수 없습니다.") @Size(message = "name 은 20자리 이하여야 합니다.", max = 20)
    String name,
    @NotNull(message = "rowCount 가 공백일 수 없습니다.") @Min(value = 1, message = "행 개수 값은 1 이상이어야 합니다.") @Max(value = 30, message = "행 개수 값은 30 이하이어야 합니다.")
    Integer rowCount,
    @NotNull(message = "열 개수 값은 필수입니다.") @Min(value = 1, message = "열 개수 값은 1 이상이어야 합니다.") @Max(value = 300, message = "열 개수 값은 300 이하이어야 합니다.")
    Integer columnCount
) {

}
