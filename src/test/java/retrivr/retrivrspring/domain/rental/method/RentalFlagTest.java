package retrivr.retrivrspring.domain.rental.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

@ExtendWith(MockitoExtension.class)
class RentalFlagTest {

  @Test
  @DisplayName("isOverdue(): dueDate가 null이면 false")
  void isOverdue_dueDateNull_false() {
    Rental rental = Rental.builder()
        .organization(mock(Organization.class))
        .borrower(mock(Borrower.class))
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    ReflectionTestUtils.setField(rental, "dueDate", null);

    assertThat(rental.isOverdue()).isFalse();
  }

  @Test
  @DisplayName("isOverdue(): dueDate가 오늘보다 이전이면 true")
  void isOverdue_beforeToday_true() {
    Rental rental = Rental.builder()
        .organization(mock(Organization.class))
        .borrower(mock(Borrower.class))
        .status(RentalStatus.RENTED) // 현재 코드 기준(REQUESTED/RENTED/RETURNED/REJECTED)
        .requestedAt(LocalDateTime.now())
        .build();

    ReflectionTestUtils.setField(rental, "dueDate", LocalDate.now().minusDays(1));

    assertThat(rental.isOverdue()).isTrue();
  }

  @Test
  @DisplayName("isOverdue(): dueDate가 오늘이면 false")
  void isOverdue_today_false() {
    Rental rental = Rental.builder()
        .organization(mock(Organization.class))
        .borrower(mock(Borrower.class))
        .status(RentalStatus.RENTED) // 현재 코드 기준(REQUESTED/RENTED/RETURNED/REJECTED)
        .requestedAt(LocalDateTime.now())
        .build();

    ReflectionTestUtils.setField(rental, "dueDate", LocalDate.now());

    assertThat(rental.isOverdue()).isFalse();
  }
}
