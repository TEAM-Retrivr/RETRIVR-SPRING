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
              request.content().getMessage(),
              sentDate
          )
      );
    }
  }
}
