package retrivr.retrivrspring.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;

import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class RentalNotificationEventListener {

  private final RentalRepository rentalRepository;
  private final SendMessageService sendMessageService;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRequested(RentalRequestedEvent event) {
    handleNotification(event.rentalId(), this::sendRequestCompleted, "request completed");
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalApproved(RentalApprovedEvent event) {
    handleNotification(event.rentalId(), this::sendRentalApproved, "rental approved");
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalRejected(RentalRejectedEvent event) {
    handleNotification(event.rentalId(), this::sendRentalRejected, "rental rejected");
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRentalReturned(RentalReturnedEvent event) {
    handleNotification(event.rentalId(), this::sendReturnConfirmed, "return confirmed");
  }

  private void handleNotification(Long rentalId, Consumer<Rental> sender, String notificationType) {
    try {
      rentalRepository.findFetchBorrowerRentalItemAndOrganizationById(rentalId)
          .ifPresentOrElse(
              rental -> sender.accept(loadRentalWithItemUnits(rental)),
              () -> log.warn("Skip {} notification. rentalId={} not found", notificationType, rentalId)
          );
    } catch (Exception e) {
      log.error("{} notification failed. rentalId={}", notificationType, rentalId, e);
    }
  }

  // 이벤트 리스너에서 별도 트랜잭션으로 실행되므로 LAZY 연관 객체 접근 시 예외 방지를 위해 미리 fetch
  private Rental loadRentalWithItemUnits(Rental rental) {
    rentalRepository.findFetchRentalItemUnitsByRentalIn(java.util.List.of(rental));
    return rental;
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
