package retrivr.retrivrspring.domain.entity.rental.state;

import java.time.LocalDate;
import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalState {
  public void approve(Rental rental, String adminName, Organization org);
  public void reject(Rental rental, String adminName, Organization org);
  public void changeDueDate(Rental rental, LocalDate newDueDate, LocalDate today);
  public void markReturned(Rental rental, LocalDateTime now);
}
