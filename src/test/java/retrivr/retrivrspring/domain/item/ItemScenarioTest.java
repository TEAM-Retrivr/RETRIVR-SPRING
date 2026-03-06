package retrivr.retrivrspring.domain.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

class ItemScenarioTest extends ItemTestFixture {

  @Nested
  @DisplayName("onRentalRequested")
  class OnRentalRequestedTest {

    @Test
    @DisplayName("NON_UNIT 물품은 itemUnit 없이 요청하면 availableQuantity가 1 감소한다")
    void nonUnitRequestWithoutItemUnit() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);

      item.onRentalRequested(null);

      assertThat(item.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("UNIT 물품은 itemUnit과 함께 요청하면 availableQuantity가 1 감소하고 itemUnit은 RENTAL_PENDING이 된다")
    void unitRequestWithItemUnit() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.AVAILABLE);

      item.onRentalRequested(itemUnit);

      assertThat(item.getAvailableQuantity()).isEqualTo(2);
      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.RENTAL_PENDING);
    }

    @Test
    @DisplayName("UNIT 물품인데 itemUnit 없이 요청하면 예외가 발생한다")
    void throwsWhenUnitTypeButItemUnitIsNull() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);

      assertThatThrownBy(() -> item.onRentalRequested(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
    }

    @Test
    @DisplayName("NON_UNIT 물품인데 itemUnit을 전달하면 예외가 발생한다")
    void throwsWhenNonUnitTypeButItemUnitProvided() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(() -> item.onRentalRequested(itemUnit))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
    }

    @Test
    @DisplayName("다른 Item 소속의 itemUnit을 전달하면 예외가 발생한다")
    void throwsWhenItemUnitDoesNotBelongToItem() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);
      Item otherItem = createItem(2L, ItemManagementType.UNIT, true, 10, 3);
      ItemUnit otherItemUnit = createItemUnit(100L, otherItem, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(() -> item.onRentalRequested(otherItemUnit))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
    }

    @Test
    @DisplayName("대여 불가능한 상태면 예외가 발생한다")
    void throwsWhenItemIsNotRentalAble() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 0);

      assertThatThrownBy(() -> item.onRentalRequested(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NOT_AVAILABLE_ITEM);
    }
  }

  @Nested
  @DisplayName("onRentalApprove")
  class OnRentalApproveTest {

    @Test
    @DisplayName("NON_UNIT 물품은 approve 시 수량이 바뀌지 않는다")
    void nonUnitApproveDoesNotChangeQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);

      item.onRentalApprove(null);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("UNIT 물품은 approve 시 itemUnit이 RENTED로 변경되고 수량은 바뀌지 않는다")
    void unitApproveChangesItemUnitOnly() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.RENTAL_PENDING);

      item.onRentalApprove(itemUnit);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.RENTED);
    }

    @Test
    @DisplayName("UNIT 물품은 approve 시 itemUnit이 RENTAL_PENDING 상태가 아니면 예외가 발생한다")
    void unitApproveChangesItemUnitStatusNotRentalPending() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.AVAILABLE);

      assertThatThrownBy(() -> item.onRentalApprove(itemUnit))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);

    }

    @Test
    @DisplayName("UNIT 물품인데 itemUnit 없이 approve 하면 예외가 발생한다")
    void throwsWhenApproveUnitWithoutItemUnit() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);

      assertThatThrownBy(() -> item.onRentalApprove(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
    }

    @Test
    @DisplayName("NON_UNIT 물품인데 itemUnit을 전달하면 예외가 발생한다")
    void throwsWhenApproveNonUnitWithItemUnit() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.RENTAL_PENDING);

      assertThatThrownBy(() -> item.onRentalApprove(itemUnit))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
    }
  }

  @Nested
  @DisplayName("onRentalRejected")
  class OnRentalRejectedTest {

    @Test
    @DisplayName("NON_UNIT 물품은 reject 시 availableQuantity가 1 증가한다")
    void nonUnitRejectIncreasesQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 2);

      item.onRentalRejected(null);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("UNIT 물품은 reject 시 availableQuantity가 1 증가하고 itemUnit은 AVAILABLE이 된다")
    void unitRejectIncreasesQuantityAndMakesUnitAvailable() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 2);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.RENTAL_PENDING);

      item.onRentalRejected(itemUnit);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("availableQuantity가 totalQuantity와 같으면 reject 시 overflow 예외가 발생한다")
    void throwsOverflowWhenRejectWouldExceedTotalQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 3, 3);

      assertThatThrownBy(() -> item.onRentalRejected(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION);
    }
  }

  @Nested
  @DisplayName("onRentalReturned")
  class OnRentalReturnedTest {

    @Test
    @DisplayName("NON_UNIT 물품은 return 시 availableQuantity가 1 증가한다")
    void nonUnitReturnIncreasesQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 2);

      item.onRentalReturned(null);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("UNIT 물품은 return 시 availableQuantity가 1 증가하고 itemUnit은 AVAILABLE이 된다")
    void unitReturnIncreasesQuantityAndMakesUnitAvailable() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 2);
      ItemUnit itemUnit = createItemUnit(100L, item, ItemUnitStatus.RENTED);

      item.onRentalReturned(itemUnit);

      assertThat(item.getAvailableQuantity()).isEqualTo(3);
      assertThat(itemUnit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
    }

    @Test
    @DisplayName("UNIT 물품인데 itemUnit 없이 return 하면 예외가 발생한다")
    void throwsWhenReturnUnitWithoutItemUnit() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 2);

      assertThatThrownBy(() -> item.onRentalReturned(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
    }
  }
}