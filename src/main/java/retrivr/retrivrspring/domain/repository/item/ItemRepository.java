package retrivr.retrivrspring.domain.repository.item;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Item i where i.id = :itemId")
  Optional<Item> findByIdForUpdate(@Param("itemId") Long itemId);

  // 유닛 라벨 수정 시 부모 물품에 PESSIMISTIC_WRITE 락을 잡은 뒤 활성 유닛을 조회한다.
  // 같은 물품의 수정 요청은 순차 처리되어, 앞선 변경을 반영한 상태로 중복을 검사한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Item i where i.id = :itemId and i.organization.id = :organizationId")
  Optional<Item> findByIdAndOrganizationIdForUpdate(@Param("itemId") Long itemId,
      @Param("organizationId") Long organizationId);

  @EntityGraph(attributePaths = "itemUnits")
  List<Item> findFetchItemUnitsByOrganization(Organization organization);
}
