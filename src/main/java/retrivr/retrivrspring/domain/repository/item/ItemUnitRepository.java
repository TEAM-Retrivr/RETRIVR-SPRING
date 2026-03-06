package retrivr.retrivrspring.domain.repository.item;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitLookupRepository;

public interface ItemUnitRepository extends JpaRepository<ItemUnit, Long>,
    ItemUnitLookupRepository {

  List<ItemUnit> findAllByItemId(Long itemId);
}
