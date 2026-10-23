package retrivr.retrivrspring.domain.repository.locker;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface LockerZoneRepository extends JpaRepository<LockerZone, UUID> {

  List<LockerZone> findAllByOrganizationOrderBySortOrderAsc(
      Organization organization);

  Optional<LockerZone> findByIdAndOrganization(UUID id, Organization organizationId);

  boolean existsByOrganizationAndNameAndIdNot(Organization organization, String name, UUID id);

  boolean existsByOrganizationAndName(Organization organization, String name);

  @Query(
      "select coalesce(min(z.sortOrder), 0) "
          + "from LockerZone z "
          + "where z.organization.id = :organizationId"
  )
  int minimumSortOrder(@Param("organizationId") Long organizationId);

  @Query(
      "select coalesce(max(z.sortOrder), -1) "
          + "from LockerZone z "
          + "where z.organization.id = :organizationId"
  )
  int maximumSortOrder(@Param("organizationId") Long organizationId);

  long countByOrganization(Organization organization);
}
