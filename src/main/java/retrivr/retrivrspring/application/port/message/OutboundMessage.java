package retrivr.retrivrspring.application.port.message;

import retrivr.retrivrspring.domain.message.MessageContent;

public record OutboundMessage(
    String phone,
    MessageContent content
) {}