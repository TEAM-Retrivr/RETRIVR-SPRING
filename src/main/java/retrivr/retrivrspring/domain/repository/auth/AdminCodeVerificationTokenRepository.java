package retrivr.retrivrspring.domain.repository.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import retrivr.retrivrspring.domain.entity.organization.AdminCodeVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;

public interface AdminCodeVerificationTokenRepository extends
    JpaRepository<AdminCodeVerificationToken, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AdminCodeVerificationToken> findForUpdateByOrganizationAndPurpose(Organization organization,
      AdminCodeVerificationPurpose purpose);

  Optional<AdminCodeVerificationToken> findByOrganizationAndPurpose(Organization organization,
      AdminCodeVerificationPurpose purpose);
}
