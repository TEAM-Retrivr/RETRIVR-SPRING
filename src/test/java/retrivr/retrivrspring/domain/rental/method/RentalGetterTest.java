package retrivr.retrivrspring.domain.rental.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RentalGetterTest {
  @Test
  @DisplayName("getItem(): rentalItems 비어있으면 INVALID_RENTAL_EXCEPTION")
  void getItem_empty_throws() {
    // given (rentalItems 없음)
    Rental rental = Rental.builder()
        .organization(mock(Organization.class))
        .borrower(mock(Borrower.class))
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    // when & then
    assertThatThrownBy(rental::getItem)
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_RENTAL_EXCEPTION);
  }

  @Test
  @DisplayName("getItemUnit(): itemUnit 없으면 null 반환")
  void getItemUnit_whenNoUnit_returnsNull() {
    // given (itemUnit 없음)
    Rental rental = Rental.builder()
        .organization(mock(Organization.class))
        .borrower(mock(Borrower.class))
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    // when
    ItemUnit unit = rental.getItemUnit();

    // then
    assertThat(unit).isNull();
  }
}
