package retrivr.retrivrspring.domain.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

class ItemPropertyTest extends ItemTestFixture {

  @Nested
  @DisplayName("isRentalAble")
  class IsRentalAbleTest {

    @Test
    @DisplayName("활성 상태이고 availableQuantity가 1 이상이면 true를 반환한다")
    void returnsTrueWhenActiveAndHasAvailableQuantity() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 3);

      assertThat(item.isRentalAble()).isTrue();
    }

    @Test
    @DisplayName("비활성 상태면 false를 반환한다")
    void returnsFalseWhenInactive() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, false, 10, 3);

      assertThat(item.isRentalAble()).isFalse();
    }

    @Test
    @DisplayName("availableQuantity가 0이면 false를 반환한다")
    void returnsFalseWhenNoAvailableQuantity() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 0);

      assertThat(item.isRentalAble()).isFalse();
    }
  }

  @Nested
  @DisplayName("activation and deletion")
  class ActivationAndDeletionTest {

    @Test
    void deactivationStopsNewRentalsAndActivationRestoresAvailability() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 3);

      item.deactivate();
      assertThat(item.isRentalAble()).isFalse();

      item.activate();
      assertThat(item.isRentalAble()).isTrue();
    }

    @Test
    void deletionDeactivatesItemAndRecordsDeletionTime() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 3);

      item.delete();

      assertThat(item.isDeleted()).isTrue();
      assertThat(item.getDeletedAt()).isNotNull();
      assertThat(item.isActive()).isFalse();
      assertThat(item.isRentalAble()).isFalse();
    }

    @Test
    void deletedItemCannotBeReactivated() {
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 3);
      item.delete();

      assertThatThrownBy(item::activate)
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ALREADY_DELETE_EXCEPTION);
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
      Item item = createItem(1L, ItemManagementType.NON_UNIT, true, 10, 3);

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

  @Nested
  @DisplayName("ItemUnit 변경")
  class ItemUnitChangeTest {

    @Test
    @DisplayName("앞뒤 공백을 제거한 label이 같으면 ItemUnit을 생성할 수 없다")
    void rejectsUnitsWithDuplicatedTrimmedLabels() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 2, 2);

      assertThatThrownBy(() -> item.createUnits(List.of(" unit-a ", "unit-a")))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.DUPLICATE_ITEM_UNIT_LABEL);
    }

    @Test
    @DisplayName("대소문자가 다르면 서로 다른 ItemUnit 이름으로 생성한다")
    void createsUnitsWithCaseSensitiveLabels() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 2, 2);

      List<ItemUnit> createdUnits = item.createUnits(List.of(" Unit-A ", "unit-a"));

      assertThat(createdUnits).extracting("label")
          .containsExactly("Unit-A", "unit-a");
    }

    @Test
    @DisplayName("동일한 label의 ItemUnit 중 요청한 ID만 삭제 대상으로 선택한다")
    void resolvesDeletableUnitByItemUnitId() {
      Item item = createItem(1L, ItemManagementType.UNIT, true, 2, 2);
      ItemUnit firstUnit = createItemUnit(101L, item, ItemUnitStatus.AVAILABLE);
      ItemUnit secondUnit = createItemUnit(102L, item, ItemUnitStatus.AVAILABLE);
      ReflectionTestUtils.setField(firstUnit, "label", "same-label");
      ReflectionTestUtils.setField(secondUnit, "label", "same-label");

      List<ItemUnit> deletableUnits =
          item.getDeletableUnits(List.of(firstUnit, secondUnit), List.of(firstUnit));

      assertThat(deletableUnits).containsExactly(firstUnit);
    }
  }
}
