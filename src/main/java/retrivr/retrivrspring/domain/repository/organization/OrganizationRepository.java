package retrivr.retrivrspring.domain.repository.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.organization.Organization;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import retrivr.retrivrspring.infrastructure.repository.organization.OrganizationSearchRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long>,
    OrganizationSearchRepository {
    Optional<Organization> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select organization from Organization organization where organization.id = :id")
    Optional<Organization> findByIdForUpdate(@Param("id") Long id);
}
