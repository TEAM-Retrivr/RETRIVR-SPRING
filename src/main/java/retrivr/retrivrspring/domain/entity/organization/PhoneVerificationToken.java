package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PhoneVerificationToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @OneToOne
  @JoinColumn(name = "phone_verification_id", nullable = false)
  private PhoneVerification phoneVerification;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  private static final int EXPIRATION_TIME_HOURS = 1;

  public static PhoneVerificationToken create(PhoneVerification phoneVerification, String tokenHash,
      LocalDateTime now) {
    return PhoneVerificationToken.builder()
        .phoneVerification(phoneVerification)
        .tokenHash(tokenHash)
        .expiresAt(now.plusHours(EXPIRATION_TIME_HOURS))
        .build();
  }

  public void refresh(String encodedToken, LocalDateTime now) {
    this.tokenHash = encodedToken;
    this.expiresAt = now.plusHours(EXPIRATION_TIME_HOURS);
  }

  public boolean isExpired(LocalDateTime now) {
    return !now.isBefore(this.expiresAt);
  }

  public void delete() {
    this.phoneVerification = null;
  }
}
