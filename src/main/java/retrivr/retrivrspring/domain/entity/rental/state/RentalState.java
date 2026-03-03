package retrivr.retrivrspring.domain.entity.rental.state;

import java.time.LocalDate;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalState {
  public void approve(Rental rental, String adminName, Organization org);
  public void reject(Rental rental, String adminName, Organization org);
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org);
  public void markReturned(Rental rental, String adminName, Organization org);
}
