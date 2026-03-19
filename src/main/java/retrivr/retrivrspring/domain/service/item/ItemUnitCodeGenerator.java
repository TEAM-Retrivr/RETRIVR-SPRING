package retrivr.retrivrspring.domain.service.item;

import retrivr.retrivrspring.domain.entity.item.Item;

public interface ItemUnitCodeGenerator {

  String generate(Item item);
}
