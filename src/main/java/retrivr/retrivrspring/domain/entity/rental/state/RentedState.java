package retrivr.retrivrspring.domain.entity.rental.state;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
  public void changeDueDate(Rental rental, LocalDate newDueDate, LocalDate today) {

  }

  @Override
  public void markReturned(Rental rental, LocalDateTime now) {

  }
}
