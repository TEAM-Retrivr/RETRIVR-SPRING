package retrivr.retrivrspring.domain.repository.membership.pass;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface MembershipPassRepository extends JpaRepository<MembershipPass, String> {

  Optional<MembershipPass> findFirstByOrganizationOrderBySequenceDesc(Organization organization);

  Optional<MembershipPass> findFirstByOrganizationAndStatusOrderBySequenceDesc(Organization organization, MembershipPassStatus status);

  Optional<MembershipPass> findFirstByOrganizationAndStatusOrderBySequenceAsc(Organization organization, MembershipPassStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select mp
      from MembershipPass mp
      join fetch mp.organization o
      where mp.status = :status
        and mp.endAt <= :now
      order by mp.endAt asc
  """)
  List<MembershipPass> findExpiredActivePassesForUpdate(
      @Param("status") MembershipPassStatus status,
      @Param("now") LocalDateTime now
  );
}
