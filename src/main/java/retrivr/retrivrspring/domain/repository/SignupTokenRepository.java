package retrivr.retrivrspring.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;

import java.util.Optional;

public interface SignupTokenRepository extends JpaRepository<SignupToken, Long> {

    Optional<SignupToken> findByEmail(String email);

    void deleteByEmail(String email);
}