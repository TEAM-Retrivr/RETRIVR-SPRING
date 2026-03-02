package retrivr.retrivrspring.infrastructure.repository.item;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;

public interface ItemUnitLookupRepository {

  List<ItemUnit> searchRentedUnitsByItemId(Long itemId, Long cursor, int limit);
}
