package retrivr.retrivrspring.infrastructure.repository.item;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemLookupRepository {

  @EntityGraph(attributePaths = "{itemBorrowerFields}")
  Optional<Item> findFetchItemBorrowerFieldsById(Long itemId);
}
