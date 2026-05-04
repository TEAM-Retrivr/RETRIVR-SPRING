package retrivr.retrivrspring.application.service.message;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageHistory;
import retrivr.retrivrspring.domain.repository.message.MessageHistoryRepository;

@Component
@RequiredArgsConstructor
public class NotificationHistoryRecorder {

  private final MessageHistoryRepository messageHistoryRepository;

  public void record(
      Rental rental,
      NotificationRequest request,
      NotificationDispatchResult result,
      LocalDate sentDate
  ) {
    for (NotificationDispatchAttempt attempt : result.attempts()) {
      messageHistoryRepository.save(
          MessageHistory.create(
              rental,
              attempt.recipient(),
              request.messageType(),
              attempt.status(),
              resolveContent(request, attempt),
              sentDate
          )
      );
    }
  }

  //todo -- 구조 개선 가능할거 같은데 getMessage()와 getTemplateMessage()를 분리하지 않는 방법 생각
  private String resolveContent(NotificationRequest request, NotificationDispatchAttempt attempt) {
    return switch (attempt.channel()) {
      case EMAIL -> request.content().getMessage();
      case ALIM_TALK -> request.content().getTemplateMessage();
    };
  }
}
