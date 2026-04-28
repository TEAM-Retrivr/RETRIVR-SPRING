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
import retrivr.retrivrspring.global.error.InfraException;

@Component
@Slf4j
public class NotificationDispatcher {

  private static final NotificationChannel DEFAULT_CHANNEL = NotificationChannel.ALIM_TALK;

  private final Map<NotificationChannel, MessageSender> messageSenders;

  public NotificationDispatcher(List<MessageSender> messageSenders) {
    this.messageSenders = toMessageSenderMap(messageSenders);
  }

  public NotificationDispatchResult dispatch(NotificationRequest request, Rental rental) {
    NotificationChannel channel = DEFAULT_CHANNEL;

    if (!request.supports(channel)) {
      handleMissingRecipient(request.messageType(), rental, channel);
      return new NotificationDispatchResult(List.of());
    }

    String recipient = request.resolveRecipient(channel);
    MessageSender sender = getMessageSender(channel);
    try {
      sender.send(request);
      return new NotificationDispatchResult(List.of(
          new NotificationDispatchAttempt(
              channel,
              recipient,
              MessageSendStatus.SUCCESS
          )
      ));
    } catch (ApplicationException | InfraException e) {
      if (e instanceof InfraException infraException) {
        log.error("Notification send failed. rentalId={}, messageType={}, channel={}, errorCode={}, detail={}",
            rental.getId(), request.messageType(), channel,
            infraException.getErrorCode(), infraException.getDetail(), e);
      } else {
        log.error("Notification send failed. rentalId={}, messageType={}, channel={}",
            rental.getId(), request.messageType(), channel, e);
      }
      return new NotificationDispatchResult(List.of(
          new NotificationDispatchAttempt(
              channel,
              recipient,
              MessageSendStatus.FAIL
          )
      ));
    }
  }

  private MessageSender getMessageSender(NotificationChannel channel) {
    MessageSender sender = messageSenders.get(channel);
    if (sender == null) {
      throw new InfraException(
          ErrorCode.MESSAGE_SENDER_NOT_FOUND,
          "channel=" + channel
      );
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
    if (messageType == MessageType.OVERDUE_REMINDER) {
      throw new ApplicationException(resolveMissingRecipientError(channel));
    }

    log.info("Skip notification. rentalId={}, messageType={}, channel={}, reason=no recipient",
        rental.getId(), messageType, channel);
  }

  private ErrorCode resolveMissingRecipientError(NotificationChannel channel) {
    return switch (channel) {
      case EMAIL -> ErrorCode.EMAIL_NOT_FOUND;
      case ALIM_TALK -> ErrorCode.INVALID_PHONE_NUMBER_EXCEPTION;
    };
  }
}
