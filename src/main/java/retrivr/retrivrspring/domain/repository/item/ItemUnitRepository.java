package retrivr.retrivrspring.domain.repository.item;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitLookupRepository;

public interface ItemUnitRepository extends JpaRepository<ItemUnit, Long>,
    ItemUnitLookupRepository {

  List<ItemUnit> findAllByItemIdAndDeletedAtIsNull(Long itemId);

  void deleteByItem(Item item);

  Optional<ItemUnit> findByIdAndItemIdAndDeletedAtIsNull(Long itemUnitId, Long itemId);

  Optional<ItemUnit> findByIdAndItemIdAndItemOrganizationIdAndDeletedAtIsNull(Long itemUnitId, Long itemId,
      Long organizationId);
}
