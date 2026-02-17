package retrivr.retrivrspring.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, Long>, OrganizationSearchRepository {

}
