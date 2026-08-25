package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "borrow_email_verification_token")
public class BorrowEmailVerificationToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public void assertUsable(
            String requestedEmail,
            String rawToken,
            PasswordEncoder passwordEncoder,
            LocalDateTime now
    ) {
        if (!email.equals(requestedEmail)) {
            throw new DomainException(ErrorCode.BORROW_EMAIL_VERIFICATION_TOKEN_MISMATCH);
        }
        if (!now.isBefore(expiresAt)) {
            throw new DomainException(ErrorCode.BORROW_EMAIL_VERIFICATION_TOKEN_EXPIRED);
        }
        if (!passwordEncoder.matches(rawToken, tokenHash)) {
            throw new DomainException(ErrorCode.BORROW_EMAIL_VERIFICATION_TOKEN_MISMATCH);
        }
    }
}
