package retrivr.retrivrspring.domain.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;

public interface ItemBorrowerFieldRepository extends JpaRepository<ItemBorrowerField, Long> {

  void deleteByItem(Item item);
}
