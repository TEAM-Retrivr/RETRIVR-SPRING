package retrivr.retrivrspring.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;

import java.util.Optional;
import retrivr.retrivrspring.infrastructure.repository.organization.OrganizationSearchRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long>,
    OrganizationSearchRepository {
    Optional<Organization> findByEmail(String email);
}