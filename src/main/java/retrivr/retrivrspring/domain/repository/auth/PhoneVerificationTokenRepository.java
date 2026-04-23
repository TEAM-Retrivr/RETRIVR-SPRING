package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerification;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerificationToken;

public interface PhoneVerificationTokenRepository extends
    JpaRepository<PhoneVerificationToken, String> {

  Optional<PhoneVerificationToken> findByPhoneVerification(PhoneVerification phoneVerification);
}
