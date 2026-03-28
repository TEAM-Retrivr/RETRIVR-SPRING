package retrivr.retrivrspring.application.service.message;

import retrivr.retrivrspring.application.port.message.NotificationChannel;
import retrivr.retrivrspring.domain.message.MessageSendStatus;

public record NotificationDispatchAttempt(
    NotificationChannel channel,
    String recipient,
    MessageSendStatus status
) {}
