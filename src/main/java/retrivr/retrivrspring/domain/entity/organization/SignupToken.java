package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@Entity
@Table(name = "signup_token")
@NoArgsConstructor
public class SignupToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long signupTokenId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "code_verified_at")
    private LocalDateTime codeVerifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    public SignupToken(String email, String tokenHash, LocalDateTime expiresAt) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void markCodeVerified(LocalDateTime time) {
        this.codeVerifiedAt = time;
    }

    public void markUsed(LocalDateTime time) {
        this.usedAt = time;
    }

    public void updateTokenHash(String newHash) {
        this.tokenHash = newHash;
    }

    public void extendExpiry(LocalDateTime newExpiry) {
        this.expiresAt = newExpiry;
    }

    public void assertUsable(String rawSignupToken, PasswordEncoder passwordEncoder, LocalDateTime now) {
        if (this.codeVerifiedAt == null) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        if (this.expiresAt.isBefore(now)) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_EXPIRED);
        }

        if (this.usedAt != null) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_ALREADY_USED);
        }

        if (!passwordEncoder.matches(rawSignupToken, this.tokenHash)) {
            throw new ApplicationException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }
    }
}
