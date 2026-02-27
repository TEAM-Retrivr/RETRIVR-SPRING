package retrivr.retrivrspring.infrastructure.repository.item;

import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;

public interface ItemLookupRepository {

  List<Item> findPageByOrganizationWithCursor(Long organizationId, Long cursor, int limit);
}
