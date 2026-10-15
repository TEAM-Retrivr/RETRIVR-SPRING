package retrivr.retrivrspring.domain.repository.locker;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.LockerRental;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;

public interface LockerRentalRepository extends JpaRepository<LockerRental, UUID> {

  @Modifying(flushAutomatically = true)
  @Query("update LockerRental rental set rental.locker = null where rental.locker.id = :lockerId")
  int clearLocker(@Param("lockerId") UUID lockerId);

  List<LockerRental> findAllByLocker_IdInAndStatusInOrderByRequestedAtDescIdDesc(
      Collection<UUID> lockerIds, Collection<LockerRentalStatus> statuses);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update LockerRental rental
      set rental.locker = null
      where rental.locker.id in (
          select locker.id
          from Locker locker
          where locker.zone = :zone
      )
      """)
  int clearLockerByZone(@Param("zone") LockerZone zone);

  boolean existsByLockerZoneAndStatusIn(LockerZone zone, Collection<LockerRentalStatus> statuses);

  boolean existsByLocker_IdInAndStatusIn(Collection<UUID> lockerIds,
      Collection<LockerRentalStatus> statuses);

  boolean existsByLockerAndStatusIn(Locker locker, List<LockerRentalStatus> activeRentals);
}
