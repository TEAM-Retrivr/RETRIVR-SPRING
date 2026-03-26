package retrivr.retrivrspring.infrastructure.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.OutboundMessage;
import retrivr.retrivrspring.application.service.email.EmailService;

@Component
@RequiredArgsConstructor
public class EmailMessageSender implements MessageSender {

  private final EmailService emailService;

  @Override
  public void send(OutboundMessage message) throws Exception {
    emailService.sendHtmlEmail(
        message.recipient(),
        message.subject(),
        toHtml(message.content().getMessage())
    );
  }

  private String toHtml(String text) {
    return "<div style=\"font-family:Arial,sans-serif;white-space:pre-wrap;\">"
        + escapeHtml(text)
        + "</div>";
  }

  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
