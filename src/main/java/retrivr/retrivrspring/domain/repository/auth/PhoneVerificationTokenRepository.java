package retrivr.retrivrspring.domain.repository.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerification;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerificationToken;

public interface PhoneVerificationTokenRepository extends
    JpaRepository<PhoneVerificationToken, String> {

  Optional<PhoneVerificationToken> findByPhoneVerification(PhoneVerification phoneVerification);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select token
      from PhoneVerificationToken token
      join fetch token.phoneVerification
      where token.id = :tokenId
      """)
  Optional<PhoneVerificationToken> findByIdWithLock(@Param("tokenId") String tokenId);
}
