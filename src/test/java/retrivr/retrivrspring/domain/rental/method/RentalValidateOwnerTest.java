package retrivr.retrivrspring.domain.rental.method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RentalValidateOwnerTest {

  @Test
  @DisplayName("소유 조직이 다르면 ORGANIZATION_MISMATCH_EXCEPTION")
  void validateOwner_mismatch_throws() {
    // given
    Organization owner = mock(Organization.class);
    Organization other = mock(Organization.class);

    when(owner.getId()).thenReturn(1L);
    when(other.getId()).thenReturn(2L);

    Rental rental = Rental.builder()
        .organization(owner)
        .borrower(mock(Borrower.class))
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    // when & then
    assertThatThrownBy(() -> rental.validateRentalOwner(other))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  @Test
  @DisplayName("소유 조직이면 통과")
  void validateOwner_match_ok() {
    // given
    Organization owner = mock(Organization.class);
    Organization sameOwnerDifferentInstance = mock(Organization.class);
    when(owner.getId()).thenReturn(1L);
    when(sameOwnerDifferentInstance.getId()).thenReturn(1L);

    Rental rental = Rental.builder()
        .organization(owner)
        .borrower(mock(Borrower.class))
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    // when & then
    assertThatCode(() -> rental.validateRentalOwner(sameOwnerDifferentInstance))
        .doesNotThrowAnyException();
  }
}
