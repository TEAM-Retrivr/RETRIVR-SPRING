package retrivr.retrivrspring.domain.entity.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface RentalState {

  void approve(Rental rental, String adminName, Organization org);

  void reject(Rental rental, String adminName, Organization org);

  void rejectBySystem(Rental rental, String systemMessage);

  void changeDueDate(Rental rental, LocalDate newDueDate, Organization org);

  void markReturned(Rental rental, String adminName, Organization org);

  int getOverdueDays(LocalDate dueDate, LocalDateTime returnedAt);

  boolean canSendOverdueMessage(Rental rental);
}
