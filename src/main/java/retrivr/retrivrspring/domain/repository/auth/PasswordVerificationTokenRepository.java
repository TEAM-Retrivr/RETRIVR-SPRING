package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;

public interface PasswordVerificationTokenRepository
        extends JpaRepository<PasswordVerificationToken, Long> {

    Optional<PasswordVerificationToken> findByOrganizationAndPurpose(
            Organization organization,
            PasswordVerificationPurpose purpose
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from PasswordVerificationToken token
             where token.id = :tokenId
            """)
    int deleteByIdIfExists(@Param("tokenId") Long tokenId);
}
