package retrivr.retrivrspring.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByEmail(String email);
}