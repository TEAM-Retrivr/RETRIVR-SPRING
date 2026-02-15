package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDate;

public record AdminRentalDueDateUpdateResponse(
    Long rentalId,
    LocalDate updatedDueDate
) {

}
