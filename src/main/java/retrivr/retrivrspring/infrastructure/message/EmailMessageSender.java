package retrivr.retrivrspring.infrastructure.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.NotificationChannel;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.application.service.email.EmailService;

@Component
@RequiredArgsConstructor
public class EmailMessageSender implements MessageSender {

  private final EmailService emailService;

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  @Override
  public void send(NotificationRequest request) throws Exception {
    emailService.sendHtmlEmail(
        request.resolveRecipient(NotificationChannel.EMAIL),
        request.content().getSubject(),
        toHtml(request.content().getMessage())
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
