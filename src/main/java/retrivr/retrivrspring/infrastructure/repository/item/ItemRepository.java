package retrivr.retrivrspring.infrastructure.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemLookupRepository {

}
