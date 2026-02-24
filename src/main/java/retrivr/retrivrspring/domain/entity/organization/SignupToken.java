package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

import java.time.LocalDateTime;

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
}