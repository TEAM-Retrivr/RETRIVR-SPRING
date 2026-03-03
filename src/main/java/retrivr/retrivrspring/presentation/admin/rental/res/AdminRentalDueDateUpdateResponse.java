package retrivr.retrivrspring.presentation.admin.rental.res;

import java.time.LocalDate;

public record AdminRentalDueDateUpdateResponse(
    Long rentalId,
    LocalDate updatedDueDate
) {

}
