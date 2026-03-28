package retrivr.retrivrspring.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;

@Component
@Slf4j
@RequiredArgsConstructor
public class RentalNotificationEventListener {

  private final RentalRepository rentalRepository;
  private final SendMessageService sendMessageService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRequested(RentalRequestedEvent event) {
    rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(event.rentalId())
        .ifPresentOrElse(
            this::sendRequestCompletedSafely,
            () -> log.warn("Skip request completed notification. rentalId={} not found", event.rentalId())
        );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalApproved(RentalApprovedEvent event) {
    rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(event.rentalId())
        .ifPresentOrElse(
            this::sendRentalApprovedSafely,
            () -> log.warn("Skip rental approved notification. rentalId={} not found", event.rentalId())
        );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRejected(RentalRejectedEvent event) {
    rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(event.rentalId())
        .ifPresentOrElse(
            this::sendRentalRejectedSafely,
            () -> log.warn("Skip rental rejected notification. rentalId={} not found", event.rentalId())
        );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalReturned(RentalReturnedEvent event) {
    rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(event.rentalId())
        .ifPresentOrElse(
            this::sendReturnConfirmedSafely,
            () -> log.warn("Skip return confirmed notification. rentalId={} not found", event.rentalId())
        );
  }

  private void sendRequestCompletedSafely(Rental rental) {
    try {
      sendMessageService.sendRequestCompleted(rental);
    } catch (ApplicationException e) {
      log.error("Request completed notification failed. rentalId={}", rental.getId(), e);
    }
  }

  private void sendRentalApprovedSafely(Rental rental) {
    try {
      sendMessageService.sendRentalApproved(rental);
    } catch (ApplicationException e) {
      log.error("Rental approved notification failed. rentalId={}", rental.getId(), e);
    }
  }

  private void sendRentalRejectedSafely(Rental rental) {
    try {
      sendMessageService.sendRentalRejected(rental);
    } catch (ApplicationException e) {
      log.error("Rental rejected notification failed. rentalId={}", rental.getId(), e);
    }
  }

  private void sendReturnConfirmedSafely(Rental rental) {
    try {
      sendMessageService.sendReturnConfirmed(rental);
    } catch (ApplicationException e) {
      log.error("Return confirmed notification failed. rentalId={}", rental.getId(), e);
    }
  }
}
