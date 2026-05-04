package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerification;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, String> {

  Optional<PhoneVerification> findByPhoneAndPurpose(PhoneNumber phone, PhoneVerificationPurpose purpose);
}
