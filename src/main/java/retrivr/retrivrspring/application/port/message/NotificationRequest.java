package retrivr.retrivrspring.application.port.message;

import retrivr.retrivrspring.domain.message.MessageContent;
import retrivr.retrivrspring.domain.message.MessageType;

public record NotificationRequest(
    MessageType messageType,
    NotificationRecipient recipient,
    MessageContent content
) {

  public String resolveRecipient(NotificationChannel channel) {
    return recipient.resolve(channel);
  }

  public boolean supports(NotificationChannel channel) {
    return recipient.hasRecipientFor(channel);
  }
}
