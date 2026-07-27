package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;

public interface PasswordVerificationTokenRepository
        extends JpaRepository<PasswordVerificationToken, Long> {

    void deleteByOrganizationAndPurpose(
            Organization organization,
            PasswordVerificationPurpose purpose
    );

    Optional<PasswordVerificationToken> findTopByOrganizationAndPurposeOrderByCreatedAtDesc(
            Organization organization,
            PasswordVerificationPurpose purpose
    );
}
