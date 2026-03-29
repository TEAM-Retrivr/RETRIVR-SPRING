package retrivr.retrivrspring.application.event;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class RentalNotificationEventListener {

  private final RentalRepository rentalRepository;
  private final SendMessageService sendMessageService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRequested(RentalRequestedEvent event) {
    handleNotification(event.rentalId(), this::sendRequestCompleted, "request completed");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalApproved(RentalApprovedEvent event) {
    handleNotification(event.rentalId(), this::sendRentalApproved, "rental approved");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRejected(RentalRejectedEvent event) {
    handleNotification(event.rentalId(), this::sendRentalRejected, "rental rejected");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalReturned(RentalReturnedEvent event) {
    handleNotification(event.rentalId(), this::sendReturnConfirmed, "return confirmed");
  }

  private void handleNotification(Long rentalId, Consumer<Rental> sender, String notificationType) {
    try {
      rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(rentalId)
          .ifPresentOrElse(
              sender,
              () -> log.warn("Skip {} notification. rentalId={} not found", notificationType, rentalId)
          );
    } catch (Exception e) {
      log.error("{} notification failed. rentalId={}", notificationType, rentalId, e);
    }
  }

  private void sendRequestCompleted(Rental rental) {
    sendMessageService.sendRequestCompleted(rental);
  }

  private void sendRentalApproved(Rental rental) {
    sendMessageService.sendRentalApproved(rental);
  }

  private void sendRentalRejected(Rental rental) {
    sendMessageService.sendRentalRejected(rental);
  }

  private void sendReturnConfirmed(Rental rental) {
    sendMessageService.sendReturnConfirmed(rental);
  }
}
