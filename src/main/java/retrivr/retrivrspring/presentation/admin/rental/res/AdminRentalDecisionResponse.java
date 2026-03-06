package retrivr.retrivrspring.presentation.admin.rental.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalDecisionStatus;

public record AdminRentalDecisionResponse(
    Long rentalId,
    RentalDecisionStatus rentalDecisionStatus,
    String adminNameToDecide,
    LocalDateTime decisionDate
) {

}
