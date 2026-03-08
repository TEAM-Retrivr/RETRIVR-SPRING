package retrivr.retrivrspring.presentation.admin.rental.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "대여 반납 예정일 변경")
public record AdminRentalDueDateUpdateRequest(
    @Schema(
        description = "새로운 반납 예정일",
        example = "2026-03-10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "반납 예정일은 필수입니다.")
    LocalDate newReturnDueDate
) {

}
