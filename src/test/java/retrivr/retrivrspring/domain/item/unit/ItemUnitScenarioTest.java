package retrivr.retrivrspring.domain.item.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

class ItemUnitScenarioTest extends ItemUnitTestFixture {

  @Nested
  @DisplayName("onRentalRequested")
  class OnRentalRequestedTest {

    @Test
    @DisplayName("AVAILABLE 상태이면 RENTAL_PENDING으로 변경된다")
    void changesToRentalPendingFromAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      itemUnit.onRentalRequested();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.RENTAL_PENDING);
    }

    @Test
    @DisplayName("AVAILABLE 상태가 아니면 예외가 발생한다")
    void throwsWhenStatusIsNotAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTED);

      assertThatThrownBy(itemUnit::onRentalRequested)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
    }

    @Test
    @DisplayName("status가 null이면 예외가 발생한다")
    void throwsWhenStatusIsNull() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnitWithoutStatus(10L, item);

      assertThatThrownBy(itemUnit::onRentalRequested)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_ITEM_UNIT);
    }
  }

  @Nested
  @DisplayName("onRentalApprove")
  class OnRentalApproveTest {

    @Test
    @DisplayName("RENTAL_PENDING 상태이면 RENTED로 변경된다")
    void changesToRentedFromRentalPending() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTAL_PENDING);

      itemUnit.onRentalApprove();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.RENTED);
    }

    @Test
    @DisplayName("RENTAL_PENDING 상태가 아니면 예외가 발생한다")
    void throwsWhenStatusIsNotRentalPending() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(itemUnit::onRentalApprove)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("onRentalRejected")
  class OnRentalRejectedTest {

    @Test
    @DisplayName("RENTAL_PENDING 상태이면 AVAILABLE로 변경된다")
    void changesToAvailableFromRentalPending() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTAL_PENDING);

      itemUnit.onRentalRejected();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("RENTED 상태이면 AVAILABLE로 변경된다")
    void changesToAvailableFromRented() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTED);

      itemUnit.onRentalRejected();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("AVAILABLE 상태이면 예외가 발생한다")
    void throwsWhenStatusIsAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(itemUnit::onRentalRejected)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("onRentalReturned")
  class OnRentalReturnedTest {

    @Test
    @DisplayName("RENTED 상태이면 AVAILABLE로 변경된다")
    void changesToAvailableFromRented() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTED);

      itemUnit.onRentalReturned();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("RENTAL_PENDING 상태이면 AVAILABLE로 변경된다")
    void changesToAvailableFromRentalPending() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTAL_PENDING);

      itemUnit.onRentalReturned();

      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("AVAILABLE 상태이면 예외가 발생한다")
    void throwsWhenStatusIsAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(itemUnit::onRentalReturned)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
    }
  }
}