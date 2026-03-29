package retrivr.retrivrspring.application.service.message;

import java.util.List;
import retrivr.retrivrspring.domain.message.MessageSendStatus;

public record NotificationDispatchResult(
    List<NotificationDispatchAttempt> attempts
) {

  public boolean hasSuccess() {
    return attempts.stream().anyMatch(attempt -> attempt.status() == MessageSendStatus.SUCCESS);
  }
}
