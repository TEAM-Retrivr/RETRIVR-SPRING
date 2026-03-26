package retrivr.retrivrspring.application.service.message;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.OutboundMessage;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageHistory;
import retrivr.retrivrspring.domain.message.MessageSendStatus;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.message.OverdueReminderContent;
import retrivr.retrivrspring.domain.message.RequestCompletedContent;
import retrivr.retrivrspring.domain.message.SendAllOverdueReminderPolicy;
import retrivr.retrivrspring.domain.repository.message.MessageHistoryRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminMessageSendResponse;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SendMessageService {

  private final MessageSender messageSender;
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
    //todo: 하루에 몇번 전송 허용할건지 로직 필요
    
    if (!rental.canSendOverdueMessage()) {
      throw new ApplicationException(ErrorCode.DO_NOT_SEND_OVERDUE_MESSAGE);
    }

    return new AdminMessageSendResponse(
        rentalId,
        sendMessage(today, rental)
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

      sendMessage(today, rental);
    }
  }

  @Transactional
  public void sendRequestCompleted(Rental rental) {
    LocalDate today = LocalDate.now();
    String recipientEmail = rental.getBorrower().getEmail();
    if (recipientEmail == null) {
      log.info("Skip request completed email. rentalId={}, reason=no recipient email",
          rental.getId());
      return;
    }

    RequestCompletedContent content = new RequestCompletedContent(
        rental.getOrganization().getName(),
        rental.getItem().getName()
    );

    OutboundMessage message = new OutboundMessage(
        recipientEmail,
        content.getSubject(),
        content
    );

    try {
      messageSender.send(message);

      messageHistoryRepository.save(
          MessageHistory.createRequestCompletedHistory(
              rental,
              message.recipient(),
              MessageSendStatus.SUCCESS,
              message.content().getMessage(),
              today
          )
      );
    } catch (Exception e) {
      messageHistoryRepository.save(
          MessageHistory.createRequestCompletedHistory(
              rental,
              message.recipient(),
              MessageSendStatus.FAIL,
              message.content().getMessage(),
              today
          )
      );
      log.error("Request completed email send failed. rentalId={}", rental.getId(), e);
    }
  }

  private boolean sendMessage(LocalDate today, Rental rental) {
    String recipientEmail = rental.getBorrower().getEmail();
    if (recipientEmail == null) {
      throw new ApplicationException(ErrorCode.EMAIL_NOT_FOUND);
    }

    OverdueReminderContent content = new OverdueReminderContent(
        rental.getOrganization().getName(),
        rental.getItem().getName(),
        rental.getOverdueDays()
    );

    OutboundMessage message = new OutboundMessage(
        recipientEmail,
        content.getSubject(),
        content
    );

    try {
      messageSender.send(message);

      messageHistoryRepository.save(
          MessageHistory.createOverdueReminderHistory(
              rental,
              message.recipient(),
              MessageSendStatus.SUCCESS,
              message.content().getMessage(),
              today
          )
      );
      return true;
    } catch (Exception e) {
      messageHistoryRepository.save(
          MessageHistory.createOverdueReminderHistory(
              rental,
              message.recipient(),
              MessageSendStatus.FAIL,
              message.content().getMessage(),
              today
          )
      );
      log.error("연체 알림 메시지 발송 실패. rentalId={}", rental.getId(), e);
      return false;
    }
  }
}
