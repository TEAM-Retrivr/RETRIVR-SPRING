package retrivr.retrivrspring.domain.item;

import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.rental.enumerate.BorrowerFieldType;

public abstract class ItemTestFixture {

  protected Item createItem(
      Long id,
      ItemManagementType itemManagementType,
      boolean isActive,
      int totalQuantity,
      int availableQuantity
  ) {
    Item item = Item.builder()
        .name("노트북")
        .rentalDuration(7)
        .description("설명")
        .guaranteedGoods("학생증")
        .useMessageAlarmService(false)
        .totalQuantity(totalQuantity)
        .availableQuantity(availableQuantity)
        .isActive(isActive)
        .itemManagementType(itemManagementType)
        .itemUnits(new ArrayList<>())
        .itemBorrowerFields(new ArrayList<>())
        .build();

    ReflectionTestUtils.setField(item, "id", id);
    return item;
  }

  protected Item createItemWithBorrowerFields(ItemBorrowerField... fields) {
    Item item = createItem(1L, ItemManagementType.SINGLE, true, 10, 10);
    ReflectionTestUtils.setField(item, "itemBorrowerFields", new ArrayList<>(List.of(fields)));
    return item;
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

  protected ItemBorrowerField borrowerField(
      String fieldKey,
      BorrowerFieldType fieldType,
      boolean required
  ) {
    return ItemBorrowerField.builder()
        .fieldKey(fieldKey)
        .label(fieldKey + " label")
        .fieldType(fieldType)
        .isRequired(required)
        .sortOrder(1)
        .build();
  }
}