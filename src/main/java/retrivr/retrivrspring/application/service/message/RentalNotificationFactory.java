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
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

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
      case PHONE_VERIFICATION -> throw new ApplicationException(
          ErrorCode.UNSUPPORTED_NOTIFICATION_MESSAGE_TYPE,
          "Unsupported message type for rental notification: " + messageType
      );
      case REQUEST_COMPLETED -> new RequestCompletedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          resolveItemDetailName(rental),
          rental.getRequestedAt()
      );
      case RENTAL_APPROVED -> new RentalApprovedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          resolveItemDetailName(rental),
          rental.getDecidedAt(),
          rental.getDueDate(),
          rental.getItem().getRentalDuration()
      );
      case RENTAL_REJECTED -> new RentalRejectedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          resolveItemDetailName(rental),
          rental.getRequestedAt()
      );
      case RETURN_CONFIRMED -> new ReturnConfirmedContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          resolveItemDetailName(rental),
          rental.getDecidedAt(),
          rental.getReturnedAt()
      );
      case OVERDUE_REMINDER -> new OverdueReminderContent(
          rental.getOrganization().getName(),
          rental.getItem().getName(),
          resolveItemDetailName(rental),
          rental.getDecidedAt(),
          rental.getDueDate(),
          rental.getItem().getRentalDuration()
      );
    };
  }

  private String resolveItemDetailName(Rental rental) {
    if (rental.hasItemUnit() && rental.getItemUnit() != null) {
      return rental.getItemUnit().getLabel();
    }
    return rental.getItem().getName();
  }
}
