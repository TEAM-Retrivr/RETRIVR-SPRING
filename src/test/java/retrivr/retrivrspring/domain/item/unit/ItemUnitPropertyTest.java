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

class ItemUnitPropertyTest extends ItemUnitTestFixture {

  @Nested
  @DisplayName("isRentalAble")
  class IsRentalAbleTest {

    @Test
    @DisplayName("status가 AVAILABLE이면 true를 반환한다")
    void returnsTrueWhenStatusIsAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThat(itemUnit.isRentalAble()).isTrue();
    }

    @Test
    @DisplayName("status가 AVAILABLE이 아니면 false를 반환한다")
    void returnsFalseWhenStatusIsNotAvailable() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.RENTED);

      assertThat(itemUnit.isRentalAble()).isFalse();
    }

    @Test
    @DisplayName("status가 null이면 예외가 발생한다")
    void throwsWhenStatusIsNull() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnitWithoutStatus(10L, item);

      assertThatThrownBy(itemUnit::isRentalAble)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_ITEM_UNIT);
    }
  }

  @Nested
  @DisplayName("isBelongTo")
  class BelongsToTest {

    @Test
    @DisplayName("같은 Item id를 참조하면 true를 반환한다")
    void returnsTrueWhenBelongsToSameItem() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThat(itemUnit.isBelongTo(item)).isTrue();
    }

    @Test
    @DisplayName("다른 Item id를 참조하면 false를 반환한다")
    void returnsFalseWhenBelongsToDifferentItem() {
      Item item = createItem(1L);
      Item otherItem = createItem(2L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThat(itemUnit.isBelongTo(otherItem)).isFalse();
    }

    @Test
    @DisplayName("targetItem이 null이면 false를 반환한다")
    void returnsFalseWhenTargetItemIsNull() {
      Item item = createItem(1L);
      ItemUnit itemUnit = createItemUnit(10L, item, ItemUnitStatus.AVAILABLE);

      assertThat(itemUnit.isBelongTo(null)).isFalse();
    }

    @Test
    @DisplayName("자신이 참조하는 item이 null이면 예외가 발생한다")
    void throwsWhenLinkedItemIsNull() {
      ItemUnit itemUnit = createItemUnitWithoutItem(10L, ItemUnitStatus.AVAILABLE);
      Item targetItem = createItem(1L);

      assertThatThrownBy(() -> itemUnit.isBelongTo(targetItem))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_ITEM_UNIT);
    }

    @Test
    @DisplayName("자신이 참조하는 item의 id가 null이면 예외가 발생한다")
    void throwsWhenLinkedItemIdIsNull() {
      Item itemWithoutId = createItemWithoutId();
      ItemUnit itemUnit = createItemUnit(10L, itemWithoutId, ItemUnitStatus.AVAILABLE);
      Item targetItem = createItem(1L);

      assertThatThrownBy(() -> itemUnit.isBelongTo(targetItem))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_ITEM_UNIT);
    }
  }
}