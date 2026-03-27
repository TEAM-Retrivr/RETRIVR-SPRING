package retrivr.retrivrspring.application.service.message;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.NotificationChannel;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageSendStatus;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Component
@Slf4j
public class NotificationDispatcher {

  private static final NotificationChannel DEFAULT_CHANNEL = NotificationChannel.EMAIL;

  private final Map<NotificationChannel, MessageSender> messageSenders;

  public NotificationDispatcher(List<MessageSender> messageSenders) {
    this.messageSenders = toMessageSenderMap(messageSenders);
  }

  public NotificationDispatchResult dispatch(NotificationRequest request, Rental rental) {
    if (!request.supports(DEFAULT_CHANNEL)) {
      handleMissingRecipient(request.messageType(), rental, DEFAULT_CHANNEL);
      return new NotificationDispatchResult(List.of());
    }

    String recipient = request.resolveRecipient(DEFAULT_CHANNEL);
    MessageSender sender = getMessageSender(DEFAULT_CHANNEL);
    try {
      sender.send(request);
      return new NotificationDispatchResult(List.of(
          new NotificationDispatchAttempt(
              DEFAULT_CHANNEL,
              recipient,
              MessageSendStatus.SUCCESS
          )
      ));
    } catch (Exception e) {
      log.error("Notification send failed. rentalId={}, messageType={}, channel={}",
          rental.getId(), request.messageType(), DEFAULT_CHANNEL, e);
      return new NotificationDispatchResult(List.of(
          new NotificationDispatchAttempt(
              DEFAULT_CHANNEL,
              recipient,
              MessageSendStatus.FAIL
          )
      ));
    }
  }

  private MessageSender getMessageSender(NotificationChannel channel) {
    MessageSender sender = messageSenders.get(channel);
    if (sender == null) {
      throw new IllegalStateException("No MessageSender for channel " + channel);
    }
    return sender;
  }

  private Map<NotificationChannel, MessageSender> toMessageSenderMap(List<MessageSender> senders) {
    Map<NotificationChannel, MessageSender> senderMap = new EnumMap<>(NotificationChannel.class);
    for (MessageSender sender : senders) {
      senderMap.put(sender.channel(), sender);
    }
    return senderMap;
  }

  private void handleMissingRecipient(
      MessageType messageType,
      Rental rental,
      NotificationChannel channel
  ) {
    if (messageType == MessageType.OVERDUE_REMINDER && channel == NotificationChannel.EMAIL) {
      throw new ApplicationException(ErrorCode.EMAIL_NOT_FOUND);
    }

    log.info("Skip notification. rentalId={}, messageType={}, channel={}, reason=no recipient",
        rental.getId(), messageType, channel);
  }
}
