package retrivr.retrivrspring.domain.repository.item;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.infrastructure.repository.item.ItemLookupRepository;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemLookupRepository {

  @EntityGraph(attributePaths = "{itemBorrowerFields}")
  Optional<Item> findFetchItemBorrowerFieldsById(Long itemId);
}
