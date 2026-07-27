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

    void deleteByOrganizationAndPurpose(
            Organization organization,
            PasswordVerificationPurpose purpose
    );

    Optional<PasswordVerificationToken> findTopByOrganizationAndPurposeOrderByCreatedAtDesc(
            Organization organization,
            PasswordVerificationPurpose purpose
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update PasswordVerificationToken token
               set token.usedAt = :usedAt,
                   token.updatedAt = :usedAt
             where token.id = :tokenId
               and token.usedAt is null
            """)
    int markUsedIfUnused(
            @Param("tokenId") Long tokenId,
            @Param("usedAt") java.time.LocalDateTime usedAt
    );
}
