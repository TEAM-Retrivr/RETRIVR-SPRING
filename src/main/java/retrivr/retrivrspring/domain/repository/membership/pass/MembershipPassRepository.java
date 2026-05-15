package retrivr.retrivrspring.domain.repository.membership.pass;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface MembershipPassRepository extends JpaRepository<MembershipPass, String> {

  Optional<MembershipPass> findFirstByOrganizationOrderBySequenceDesc(Organization organization);

  Optional<MembershipPass> findFirstByOrganizationAndStatusOrderBySequenceDesc(Organization organization, MembershipPassStatus status);

}
