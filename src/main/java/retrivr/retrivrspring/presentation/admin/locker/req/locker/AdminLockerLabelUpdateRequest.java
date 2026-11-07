package retrivr.retrivrspring.presentation.admin.locker.req.locker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLockerLabelUpdateRequest(
    @NotBlank(message = "사물함 라벨 값은 공백일 수 없습니다.") @Size(max = 50, message = "사물함 라벨 값은 50자 이하여야 합니다.") String label
) {

}
