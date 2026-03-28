package retrivr.retrivrspring.application.service.message;

import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageType;

public interface NotificationFactory {

  NotificationRequest create(MessageType messageType, Rental rental);
}
