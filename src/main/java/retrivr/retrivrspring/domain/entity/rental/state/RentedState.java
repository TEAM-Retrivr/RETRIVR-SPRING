package retrivr.retrivrspring.domain.entity.rental.state;

import java.time.LocalDate;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

public class RentedState implements RentalState {


  @Override
  public void approve(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot approve when RENTED");
  }

  @Override
  public void reject(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when RETURNED");
  }

  @Override
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org) {
    rental.validateRentalOwner(org);
    rental.updateDueDate(newDueDate);
  }

  @Override
  public void markReturned(Rental rental, String adminName, Organization org) {
    rental.validateRentalOwner(org);
    Item item = rental.getItem();
    item.onRentalReturned(rental.getItemUnit());
    rental.setReturned(adminName, LocalDateTime.now());
  }
}
