package retrivr.retrivrspring.domain.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

class ItemPropertyTest extends ItemTestFixture {

  @Nested
  @DisplayName("isRentalAble")
  class IsRentalAbleTest {

    @Test
    @DisplayName("활성 상태이고 availableQuantity가 1 이상이면 true를 반환한다")
    void returnsTrueWhenActiveAndHasAvailableQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);

      assertThat(item.isRentalAble()).isTrue();
    }

    @Test
    @DisplayName("비활성 상태면 false를 반환한다")
    void returnsFalseWhenInactive() {
      Item item = createItem(1L, ItemManagementType.SINGLE, false, 10, 3);

      assertThat(item.isRentalAble()).isFalse();
    }

    @Test
    @DisplayName("availableQuantity가 0이면 false를 반환한다")
    void returnsFalseWhenNoAvailableQuantity() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 0);

      assertThat(item.isRentalAble()).isFalse();
    }
  }

  @Nested
  @DisplayName("isUnitType")
  class IsUnitTypeTest {

    @Test
    @DisplayName("itemManagementType이 UNIT이면 true를 반환한다")
    void returnsTrueWhenUnitType() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 10, 3);

      assertThat(item.isUnitType()).isTrue();
    }

    @Test
    @DisplayName("itemManagementType이 SINGLE이면 false를 반환한다")
    void returnsFalseWhenSingleType() {
      Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 3);

      assertThat(item.isUnitType()).isFalse();
    }

    @Test
    @DisplayName("itemManagementType이 null이면 예외가 발생한다")
    void throwsWhenItemManagementTypeIsNull() {
      Item item = createItem(1L, null, true, 10, 3);

      assertThatThrownBy(item::isUnitType)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_ITEM);
    }
  }
}