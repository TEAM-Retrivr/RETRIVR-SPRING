package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.AdminCodeVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;

public interface AdminCodeVerificationTokenRepository extends
    JpaRepository<AdminCodeVerificationToken, Long> {

  Optional<AdminCodeVerificationToken> findByOrganizationAndPurpose(Organization organization,
      AdminCodeVerificationPurpose purpose);
}
