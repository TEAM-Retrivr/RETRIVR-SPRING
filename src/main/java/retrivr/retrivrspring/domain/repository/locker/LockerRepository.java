package retrivr.retrivrspring.domain.repository.locker;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerEnableStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerOperationStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.infrastructure.repository.locker.LockerSearchRepository;

public interface LockerRepository extends JpaRepository<Locker, UUID>, JpaSpecificationExecutor<Locker>,
    LockerSearchRepository {

  @Modifying
  @Query("delete from Locker locker where locker.zone = :zone")
  int deleteAllByZoneId(@Param("zone") LockerZone zone);

  List<Locker> findAllByZoneOrderByRowIndexAscColumnIndexAsc(LockerZone zone);

  Optional<Locker> findByIdAndZoneOrganization(UUID id, Organization organization);

  @EntityGraph(attributePaths = "zone")
  List<Locker> findAllByIdInAndZoneOrganization(Collection<UUID> ids, Organization organization);

  boolean existsByZoneAndLabelAndIdNot(LockerZone zone, String label, UUID id);

  @Override
  @EntityGraph(attributePaths = "zone")
  Page<Locker> findAll(Specification<Locker> specification, Pageable pageable);

  @Query("""
      select l.enableStatus as enableStatus, l.operationStatus as operationStatus, count(l) as quantity
      from Locker l
      where l.zone.organization.id = :organizationId and l.zone = :zone
      group by l.enableStatus, l.operationStatus
      """)
  List<StatusCount> countByStatus(@Param("organizationId") Long organizationId, @Param("zoneId") String zoneId);

  List<Locker> findByZoneAndOperationStatusOrderByRowIndexAscColumnIndexAsc(LockerZone zone, LockerOperationStatus operationStatus);

  interface StatusCount {
    LockerEnableStatus getEnableStatus();
    LockerOperationStatus getOperationStatus();
    long getQuantity();
  }
}
