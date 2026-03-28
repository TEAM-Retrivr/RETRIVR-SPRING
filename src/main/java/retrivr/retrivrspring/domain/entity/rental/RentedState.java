package retrivr.retrivrspring.domain.entity.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RentedState implements RentalState {

  protected static final RentedState INSTANCE = new RentedState();

  @Override
  public void approve(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot approve when RENTED");
  }

  @Override
  public void reject(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when RENTED");
  }

  @Override
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org) {
    rental.validateRentalOwner(org);
    if (newDueDate == null) {
      throw new DomainException(ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION, "Cannot change due date to null");
    }
    rental.setDueDateInternal(newDueDate);
  }

  @Override
  public void markReturned(Rental rental, String adminName, Organization org) {
    rental.validateRentalOwner(org);
    Item item = rental.getItem();
    item.onRentalReturned(rental.getItemUnit());
    rental.setReturned(adminName, LocalDateTime.now());
  }

  @Override
  public int getOverdueDays(LocalDate dueDate, LocalDateTime returnedAt) {
    long days = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    return (int) Math.max(days, 0);
  }

  @Override
  public boolean canSendOverdueMessage(Rental rental) {
    return rental.getBorrower().isValidPhoneFormat();
  }
}
