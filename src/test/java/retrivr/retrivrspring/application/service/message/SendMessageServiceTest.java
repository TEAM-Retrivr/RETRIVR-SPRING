package retrivr.retrivrspring.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.application.port.message.NotificationChannel;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageSendStatus;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.repository.message.MessageHistoryRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class SendMessageServiceTest {

  @Mock
  private NotificationFactory notificationFactory;
  @Mock
  private NotificationDispatcher notificationDispatcher;
  @Mock
  private NotificationHistoryRecorder notificationHistoryRecorder;
  @Mock
  private MessageHistoryRepository messageHistoryRepository;
  @Mock
  private RentalRepository rentalRepository;
  @Mock
  private OrganizationRepository organizationRepository;

  private SendMessageService service() {
    return new SendMessageService(
        notificationFactory,
        notificationDispatcher,
        notificationHistoryRecorder,
        messageHistoryRepository,
        rentalRepository,
        organizationRepository
    );
  }

  @Test
  @DisplayName("dispatch: request completed uses email recipient and saves success history")
  void dispatch_requestCompleted_success() {
    Rental rental = mockRental("user@test.com", "01012345678");
    NotificationRequest request = mock(NotificationRequest.class);
    NotificationDispatchResult result = new NotificationDispatchResult(
        java.util.List.of(new NotificationDispatchAttempt(
            NotificationChannel.EMAIL,
            "user@test.com",
            MessageSendStatus.SUCCESS
        ))
    );

    when(notificationFactory.create(MessageType.REQUEST_COMPLETED, rental)).thenReturn(request);
    when(notificationDispatcher.dispatch(request, rental)).thenReturn(result);

    boolean sent = service().dispatch(MessageType.REQUEST_COMPLETED, rental);

    assertThat(sent).isTrue();
    verify(notificationFactory).create(MessageType.REQUEST_COMPLETED, rental);
    verify(notificationDispatcher).dispatch(request, rental);
    verify(notificationHistoryRecorder).record(rental, request, result, LocalDate.now());
  }

  @Test
  @DisplayName("dispatch: request completed skips when email is missing")
  void dispatch_requestCompleted_skipWhenEmailMissing() {
    Rental rental = mockRental(null, "01012345678");
    NotificationRequest request = mock(NotificationRequest.class);
    NotificationDispatchResult result = new NotificationDispatchResult(java.util.List.of());

    when(notificationFactory.create(MessageType.REQUEST_COMPLETED, rental)).thenReturn(request);
    when(notificationDispatcher.dispatch(request, rental)).thenReturn(result);

    boolean sent = service().dispatch(MessageType.REQUEST_COMPLETED, rental);

    assertThat(sent).isFalse();
    verify(notificationHistoryRecorder).record(rental, request, result, LocalDate.now());
  }

  @Test
  @DisplayName("sendRentalRejected: dispatches rejected notification")
  void sendRentalRejected_dispatch() {
    Rental rental = mock(Rental.class);
    NotificationRequest request = mock(NotificationRequest.class);
    NotificationDispatchResult result = new NotificationDispatchResult(
        java.util.List.of(new NotificationDispatchAttempt(
            NotificationChannel.EMAIL,
            "user@test.com",
            MessageSendStatus.SUCCESS
        ))
    );

    when(notificationFactory.create(MessageType.RENTAL_REJECTED, rental)).thenReturn(request);
    when(notificationDispatcher.dispatch(request, rental)).thenReturn(result);

    service().sendRentalRejected(rental);

    verify(notificationFactory).create(MessageType.RENTAL_REJECTED, rental);
    verify(notificationDispatcher).dispatch(request, rental);
    verify(notificationHistoryRecorder).record(rental, request, result, LocalDate.now());
  }

  @Test
  @DisplayName("dispatch: overdue reminder throws when email is missing")
  void dispatch_overdueReminder_throwWhenEmailMissing() {
    Rental rental = mockRental(null, "01012345678");
    NotificationRequest request = mock(NotificationRequest.class);

    when(notificationFactory.create(MessageType.OVERDUE_REMINDER, rental)).thenReturn(request);
    doThrow(new ApplicationException(ErrorCode.EMAIL_NOT_FOUND))
        .when(notificationDispatcher)
        .dispatch(request, rental);

    assertThatThrownBy(() -> service().dispatch(MessageType.OVERDUE_REMINDER, rental))
        .isInstanceOf(ApplicationException.class)
        .satisfies(exception -> assertThat(((ApplicationException) exception).getErrorCode())
            .isEqualTo(ErrorCode.EMAIL_NOT_FOUND));

    verify(notificationHistoryRecorder, never())
        .record(any(Rental.class), any(NotificationRequest.class), any(), any(LocalDate.class));
  }

  private Rental mockRental(String email, String phoneNumber) {
    return mock(Rental.class);
  }
}
