package retrivr.retrivrspring.domain.repository.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.organization.BorrowEmailVerificationToken;

public interface BorrowEmailVerificationTokenRepository
        extends JpaRepository<BorrowEmailVerificationToken, Long> {

    Optional<BorrowEmailVerificationToken> findByEmail(String email);

    void deleteByEmail(String email);
}
