package retrivr.retrivrspring.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByOrganizationOrderByCreatedAtDesc(Organization organization);
}