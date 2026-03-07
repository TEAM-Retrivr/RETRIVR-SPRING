package retrivr.retrivrspring.domain.item.unit;

import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

public abstract class ItemUnitTestFixture {

  protected Item createItem(Long id) {
    Item item = Item.builder()
        .name("노트북")
        .rentalDuration(7)
        .description("설명")
        .guaranteedGoods("학생증")
        .useMessageAlarmService(false)
        .totalQuantity(10)
        .availableQuantity(10)
        .isActive(true)
        .itemManagementType(ItemManagementType.UNIT)
        .build();

    ReflectionTestUtils.setField(item, "id", id);
    return item;
  }

  protected Item createItemWithoutId() {
    return Item.builder()
        .name("노트북")
        .rentalDuration(7)
        .description("설명")
        .guaranteedGoods("학생증")
        .useMessageAlarmService(false)
        .totalQuantity(10)
        .availableQuantity(10)
        .isActive(true)
        .itemManagementType(ItemManagementType.UNIT)
        .build();
  }

  protected ItemUnit createItemUnit(Long id, Item item, ItemUnitStatus status) {
    ItemUnit itemUnit = ItemUnit.builder()
        .item(item)
        .code("UNIT-" + id)
        .status(status)
        .build();

    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }

  protected ItemUnit createItemUnitWithoutItem(Long id, ItemUnitStatus status) {
    ItemUnit itemUnit = ItemUnit.builder()
        .item(null)
        .code("UNIT-" + id)
        .status(status)
        .build();

    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }

  protected ItemUnit createItemUnitWithoutStatus(Long id, Item item) {
    ItemUnit itemUnit = ItemUnit.builder()
        .item(item)
        .code("UNIT-" + id)
        .status(null)
        .build();

    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }
}