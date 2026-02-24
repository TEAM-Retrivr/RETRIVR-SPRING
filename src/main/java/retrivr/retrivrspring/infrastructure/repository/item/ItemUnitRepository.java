package retrivr.retrivrspring.infrastructure.repository.item;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;

public interface ItemUnitRepository extends JpaRepository<ItemUnit, Long> {

  List<ItemUnit> findAllByItemId(Long itemId);
}
