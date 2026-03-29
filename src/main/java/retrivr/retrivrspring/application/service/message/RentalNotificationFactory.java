package retrivr.retrivrspring.application.service.message;

import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.NotificationRecipient;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageContent;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.message.OverdueReminderContent;
import retrivr.retrivrspring.domain.message.RentalApprovedContent;
import retrivr.retrivrspring.domain.message.RentalRejectedContent;
import retrivr.retrivrspring.domain.message.RequestCompletedContent;
import retrivr.retrivrspring.domain.message.ReturnConfirmedContent;

@Component
public class RentalNotificationFactory implements NotificationFactory {

  @Override
  public NotificationRequest create(MessageType messageType, Rental rental) {
    return new NotificationRequest(
        messageType,
        new NotificationRecipient(
            rental.getBorrower().getEmail(),
            rental.getBorrower().getPhoneNumber()
        ),
        createContent(messageType, rental)
    );
  }

  private MessageContent createContent(MessageType messageType, Rental rental) {
    return switch (messageType) {
      case REQUEST_COMPLETED -> new RequestCompletedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName()
      );
      case RENTAL_APPROVED -> new RentalApprovedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          rental.getDueDate()
      );
      case RENTAL_REJECTED -> new RentalRejectedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName()
      );
      case RETURN_CONFIRMED -> new ReturnConfirmedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName()
      );
      case OVERDUE_REMINDER -> new OverdueReminderContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          rental.getOverdueDays()
      );
    };
  }
}
