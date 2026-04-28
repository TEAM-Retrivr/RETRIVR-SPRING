package retrivr.retrivrspring.domain.entity.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReturnedState implements RentalState {

  protected static final ReturnedState INSTANCE = new ReturnedState();

  @Override
  public void approve(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot approve when RETURNED");
  }

  @Override
  public void reject(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when RETURNED");
  }

  @Override
  public void rejectBySystem(Rental rental, String systemMessage) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when RETURNED");
  }

  @Override
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot changeDueDate when RETURNED");
  }

  @Override
  public void markReturned(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot markReturned when RETURNED");
  }

  @Override
  public int getOverdueDays(LocalDate dueDate, LocalDateTime returnedAt) {
    long days = ChronoUnit.DAYS.between(dueDate, returnedAt.toLocalDate());
    return (int) Math.max(days, 0);
  }

  @Override
  public boolean canSendOverdueMessage(Rental rental) {
    return false;
  }

  @Override
  public int getRentalPeriod(LocalDateTime decidedAt, LocalDateTime returnedAt, LocalDateTime now) {
    long days = ChronoUnit.DAYS.between(decidedAt.toLocalDate(), returnedAt.toLocalDate());
    return (int) Math.max(days, 0);
  }
}
