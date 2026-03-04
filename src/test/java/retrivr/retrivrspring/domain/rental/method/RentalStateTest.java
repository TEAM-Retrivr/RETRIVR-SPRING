package retrivr.retrivrspring.domain.rental.method;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.RejectedState;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentedState;
import retrivr.retrivrspring.domain.entity.rental.RequestedState;
import retrivr.retrivrspring.domain.entity.rental.ReturnedState;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

public class RentalStateTest {
  // --- helpers ---
  private static void assertTransitionException(Throwable t) {
    assertThat(t).isInstanceOf(DomainException.class);
    DomainException de = (DomainException) t;

    assertThat(de)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION);

  }

  @Nested
  @DisplayName("RequestedState")
  class RequestedStateTest {

    @Test
    @DisplayName("approve: 소유자 검증 → item.onRentalApprove(itemUnit) → setRented(admin, now, now+duration)")
    void approve_ok() {
      // given
      Rental rental = spy(Rental.builder().build());
      Organization org = mock(Organization.class);

      Item item = mock(Item.class);
      ItemUnit unit = mock(ItemUnit.class);

      doNothing().when(rental).validateRentalOwner(org);
      doReturn(item).when(rental).getItem();
      doReturn(unit).when(rental).getItemUnit();
      when(item.getRentalDuration()).thenReturn(7);

      ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      ArgumentCaptor<LocalDate> dueCaptor = ArgumentCaptor.forClass(LocalDate.class);

      // when
      RequestedState.INSTANCE.approve(rental, "adminA", org);

      // then
      verify(rental, times(1)).validateRentalOwner(org);
      verify(item, times(1)).onRentalApprove(unit);

      verify(rental, times(1))
          .setRented(eq("adminA"), nowCaptor.capture(), dueCaptor.capture());

      LocalDateTime now = nowCaptor.getValue();
      LocalDate due = dueCaptor.getValue();
      assertThat(due).isEqualTo(now.plusDays(7).toLocalDate());
    }

    @Test
    @DisplayName("reject: 소유자 검증 → item.onRentalRejected(itemUnit) → setRejected(admin, now)")
    void reject_ok() {
      // given
      Rental rental = spy(Rental.builder().build());
      Organization org = mock(Organization.class);

      Item item = mock(Item.class);
      ItemUnit unit = mock(ItemUnit.class);

      doNothing().when(rental).validateRentalOwner(org);
      doReturn(item).when(rental).getItem();
      doReturn(unit).when(rental).getItemUnit();

      ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

      // when
      RequestedState.INSTANCE.reject(rental, "adminB", org);

      // then
      verify(rental, times(1)).validateRentalOwner(org);
      verify(item, times(1)).onRentalRejected(unit);

      verify(rental, times(1))
          .setRejected(eq("adminB"), nowCaptor.capture());

      assertThat(nowCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("changeDueDate/markReturned/getOverdueDays 는 REQUESTED에서 불가(transition exception)")
    void requested_forbiddenTransitions() {
      Rental rental = mock(Rental.class);
      Organization org = mock(Organization.class);

      assertTransitionException(
          catchThrowable(() -> RequestedState.INSTANCE.changeDueDate(rental, LocalDate.now(), org))
      );
      assertTransitionException(
          catchThrowable(() -> RequestedState.INSTANCE.markReturned(rental, "admin", org))
      );
      assertTransitionException(
          catchThrowable(() -> RequestedState.INSTANCE.getOverdueDays(LocalDate.now(), LocalDateTime.now()))
      );
    }
  }

  @Nested
  @DisplayName("RentedState")
  class RentedStateTest {

    @Test
    @DisplayName("changeDueDate: 소유자 검증 후 dueDate 변경(null이면 RENTAL_DUE_DATE_UPDATE_EXCEPTION)")
    void changeDueDate_ok_and_null_forbidden() {
      // given
      Rental rental = spy(Rental.builder().build());
      Organization org = mock(Organization.class);

      doNothing().when(rental).validateRentalOwner(org);

      // when
      LocalDate newDue = LocalDate.now().plusDays(3);
      RentedState.INSTANCE.changeDueDate(rental, newDue, org);

      // then
      verify(rental, times(1)).validateRentalOwner(org);
      verify(rental, times(1)).setDueDateInternal(newDue);

      // null forbidden
      Throwable t = catchThrowable(() -> RentedState.INSTANCE.changeDueDate(rental, null, org));
      assertThat(t).isInstanceOf(DomainException.class);
      assertThat(t)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION);
    }

    @Test
    @DisplayName("markReturned: 소유자 검증 → item.onRentalReturned(itemUnit) → setReturned(admin, now)")
    void markReturned_ok() {
      // given
      Rental rental = spy(Rental.builder().build());
      Organization org = mock(Organization.class);

      Item item = mock(Item.class);
      ItemUnit unit = mock(ItemUnit.class);

      doNothing().when(rental).validateRentalOwner(org);
      doReturn(item).when(rental).getItem();
      doReturn(unit).when(rental).getItemUnit();

      ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

      // when
      RentedState.INSTANCE.markReturned(rental, "adminC", org);

      // then
      verify(rental, times(1)).validateRentalOwner(org);
      verify(item, times(1)).onRentalReturned(unit);
      verify(rental, times(1)).setReturned(eq("adminC"), nowCaptor.capture());
      assertThat(nowCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("getOverdueDays: dueDate가 과거면 양수, 미래면 0")
    void getOverdueDays_calc() {
      LocalDate past = LocalDate.now().minusDays(5);
      LocalDate future = LocalDate.now().plusDays(5);

      int pastDays = RentedState.INSTANCE.getOverdueDays(past, null);
      int futureDays = RentedState.INSTANCE.getOverdueDays(future, null);

      assertThat(pastDays).isGreaterThanOrEqualTo(5);
      assertThat(futureDays).isEqualTo(0);
    }

    @Test
    @DisplayName("approve/reject 는 RENTED에서 불가(transition exception)")
    void rented_forbiddenTransitions() {
      Rental rental = mock(Rental.class);
      Organization org = mock(Organization.class);

      assertTransitionException(
          catchThrowable(() -> RentedState.INSTANCE.approve(rental, "admin", org))
      );
      assertTransitionException(
          catchThrowable(() -> RentedState.INSTANCE.reject(rental, "admin", org))
      );
    }
  }

  @Nested
  @DisplayName("ReturnedState")
  class ReturnedStateTest {

    @Test
    @DisplayName("getOverdueDays: (returnedAt.toLocalDate - dueDate) days - 연체되었던 경우")
    void getOverdueDays_calc_overdue() {
      // given
      LocalDate due = LocalDate.of(2026, 3, 1);
      LocalDateTime returnedAt = LocalDateTime.of(2026, 3, 4, 10, 0);

      // when
      int days = ReturnedState.INSTANCE.getOverdueDays(due, returnedAt);

      // then
      assertThat(days).isEqualTo(3);
    }

    @Test
    @DisplayName("getOverdueDays: (returnedAt.toLocalDate - dueDate) days - 미연체 시")
    void getOverdueDays_calc_non_overdue() {
      // given: earlyReturn
      LocalDate due = LocalDate.of(2026, 3, 1);
      LocalDateTime earlyReturn = LocalDateTime.of(2026, 2, 28, 10, 0);

      // when
      int days2 = ReturnedState.INSTANCE.getOverdueDays(due, earlyReturn);

      // then
      assertThat(days2).isEqualTo(0);
    }

    @Test
    @DisplayName("approve/reject/changeDueDate/markReturned 는 RETURNED에서 불가(transition exception)")
    void returned_forbiddenTransitions() {
      Rental rental = mock(Rental.class);
      Organization org = mock(Organization.class);

      assertTransitionException(catchThrowable(() -> ReturnedState.INSTANCE.approve(rental, "admin", org)));
      assertTransitionException(catchThrowable(() -> ReturnedState.INSTANCE.reject(rental, "admin", org)));
      assertTransitionException(catchThrowable(() -> ReturnedState.INSTANCE.changeDueDate(rental, LocalDate.now(), org)));
      assertTransitionException(catchThrowable(() -> ReturnedState.INSTANCE.markReturned(rental, "admin", org)));
    }
  }

  @Nested
  @DisplayName("RejectedState")
  class RejectedStateTest {

    @Test
    @DisplayName("REJECTED는 모든 액션 불가(transition exception)")
    void rejected_allForbidden() {
      Rental rental = mock(Rental.class);
      Organization org = mock(Organization.class);

      assertTransitionException(catchThrowable(() -> RejectedState.INSTANCE.approve(rental, "admin", org)));
      assertTransitionException(catchThrowable(() -> RejectedState.INSTANCE.reject(rental, "admin", org)));
      assertTransitionException(catchThrowable(() -> RejectedState.INSTANCE.changeDueDate(rental, LocalDate.now(), org)));
      assertTransitionException(catchThrowable(() -> RejectedState.INSTANCE.markReturned(rental, "admin", org)));
      assertTransitionException(catchThrowable(() -> RejectedState.INSTANCE.getOverdueDays(LocalDate.now(), LocalDateTime.now())));
    }
  }
}
