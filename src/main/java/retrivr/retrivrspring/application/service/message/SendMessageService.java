package retrivr.retrivrspring.application.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.message.SendAllOverdueReminderPolicy;
import retrivr.retrivrspring.domain.repository.message.MessageHistoryRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminMessageSendResponse;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SendMessageService {

  private final NotificationFactory notificationFactory;
  private final NotificationDispatcher notificationDispatcher;
  private final NotificationHistoryRecorder notificationHistoryRecorder;
  private final MessageHistoryRepository messageHistoryRepository;
  private final RentalRepository rentalRepository;
  private final OrganizationRepository organizationRepository;

  @Transactional
  public AdminMessageSendResponse sendOverdueReminder(Long rentalId, Long loginOrganizationId) {
    LocalDate today = LocalDate.now();
    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    Rental rental = rentalRepository.findById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    rental.validateRentalOwner(organization);

    if (!rental.canSendOverdueMessage()) {
      throw new ApplicationException(ErrorCode.DO_NOT_SEND_OVERDUE_MESSAGE);
    }

    return new AdminMessageSendResponse(
        rentalId,
        dispatch(MessageType.OVERDUE_REMINDER, rental, today)
    );
  }

  @Transactional
  public void sendAllOverdueReminders() {
    LocalDate today = LocalDate.now();

    List<Rental> rentals = rentalRepository.findOverdueReminderTargets(today);

    for (Rental rental : rentals) {
      if (!SendAllOverdueReminderPolicy.shouldSend(rental.getOverdueDays())) {
        continue;
      }

      if (messageHistoryRepository.existsByRentalAndSentDateAndMessageType(
          rental,
          today,
          MessageType.OVERDUE_REMINDER
      )) {
        continue;
      }

      dispatch(MessageType.OVERDUE_REMINDER, rental, today);
    }
  }

  @Transactional
  public void sendRequestCompleted(Rental rental) {
    dispatch(MessageType.REQUEST_COMPLETED, rental);
  }

  @Transactional
  public void sendRentalApproved(Rental rental) {
    dispatch(MessageType.RENTAL_APPROVED, rental);
  }

  @Transactional
  public void sendReturnConfirmed(Rental rental) {
    dispatch(MessageType.RETURN_CONFIRMED, rental);
  }

  @Transactional
  public boolean dispatch(MessageType messageType, Rental rental) {
    return dispatch(messageType, rental, LocalDate.now());
  }

  private boolean dispatch(MessageType messageType, Rental rental, LocalDate sentDate) {
    var notification = notificationFactory.create(messageType, rental);
    NotificationDispatchResult result = notificationDispatcher.dispatch(notification, rental);
    notificationHistoryRecorder.record(rental, notification, result, sentDate);
    return result.hasSuccess();
  }
}
