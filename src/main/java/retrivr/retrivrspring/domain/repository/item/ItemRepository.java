package retrivr.retrivrspring.domain.repository.item;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.infrastructure.repository.item.ItemLookupRepository;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemLookupRepository {

  @EntityGraph(attributePaths = "itemBorrowerFields")
  Optional<Item> findFetchItemBorrowerFieldsById(Long itemId);

  @EntityGraph(attributePaths = "itemBorrowerFields")
  Optional<Item> findFetchItemBorrowerFieldsByIdAndOrganization_Id(Long itemId,
      Long organizationId);

  Optional<Item> findByIdAndOrganization_Id(Long itemId, Long organizationId);

  @EntityGraph(attributePaths = "itemUnits")
  List<Item> findFetchItemUnitsByOrganization(Organization organization);
}
