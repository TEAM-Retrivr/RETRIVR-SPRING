package retrivr.retrivrspring.presentation.admin.locker.req.locker;

import jakarta.validation.constraints.NotNull;

public record AdminLockerAvailabilityRequest(
    @NotNull(message = "대여 가능 상태 값은 필수입니다.") LockerRentalAbleStatus rentalAbleStatus
) {

}
