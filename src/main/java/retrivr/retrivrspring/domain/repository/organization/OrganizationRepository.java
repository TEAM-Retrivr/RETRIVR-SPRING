package retrivr.retrivrspring.domain.repository.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long>,
        OrganizationSearchRepository {
    Optional<Organization> findByEmail(String email);
}