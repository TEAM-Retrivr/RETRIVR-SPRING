package retrivr.retrivrspring.presentation.admin.rental.req;

import java.time.LocalDate;

public record AdminRentalDueDateUpdateRequest(
    LocalDate newReturnDueDate
) {

}
