package retrivr.retrivrspring.presentation.rental.res;

import java.time.LocalDateTime;

public record PublicRentalCreateResponse(
    Long rentalId,
    Long itemId,
    Long itemUnitId,
    LocalDateTime requestedAt
) {

}
