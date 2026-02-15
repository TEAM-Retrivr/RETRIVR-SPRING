package retrivr.retrivrspring.presentation.rental.req;

import java.time.LocalDate;

public record RentalItemUpdateDueDateRequest(
    LocalDate newReturnDueDate
) {

}
