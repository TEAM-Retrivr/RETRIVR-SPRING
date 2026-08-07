package retrivr.retrivrspring.infrastructure.repository.membership;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface MembershipPassSearchRepository {

  List<MembershipPass> findMembershipPassHistory(
      Organization organization,
      Long cursor,
      LocalDateTime start,
      LocalDateTime end,
      Pageable pageable
  );
}
