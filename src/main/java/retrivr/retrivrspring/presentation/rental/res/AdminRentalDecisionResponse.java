package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalDecisionStatus;

public record AdminRentalDecisionResponse(
    Long rentalId,
    RentalDecisionStatus rentalDecisionStatus,
    String adminNameToApprove,
    LocalDateTime decisionDate

) {

}
