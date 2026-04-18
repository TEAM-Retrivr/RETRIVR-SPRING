package retrivr.retrivrspring.domain.entity.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RejectedState implements RentalState {

  protected static final RejectedState INSTANCE = new RejectedState();

  @Override
  public void approve(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot approve when REJECTED");
  }

  @Override
  public void reject(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when REJECTED");
  }

  @Override
  public void rejectBySystem(Rental rental, String systemMessage) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot reject when REJECTED");
  }

  @Override
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot changeDueDate when REJECTED");
  }

  @Override
  public void markReturned(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot markReturned when REJECTED");
  }

  @Override
  public int getOverdueDays(LocalDate dueDate, LocalDateTime returnedAt) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot getOverdueDays when REJECTED");
  }

  @Override
  public boolean canSendOverdueMessage(Rental rental) {
    return false;
  }
}
