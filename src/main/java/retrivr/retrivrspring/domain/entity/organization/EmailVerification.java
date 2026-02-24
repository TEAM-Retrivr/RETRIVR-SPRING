package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "email_verification",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email", "purpose"})
        }
)
public class EmailVerification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "email_verification_id")
  private Long id;

  @Column(nullable = false, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EmailVerificationPurpose purpose;

  @Column(nullable = false, length = 255)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  public static EmailVerification create(
          String email,
          EmailVerificationPurpose purpose,
          String hashedCode,
          LocalDateTime expiresAt
  ) {
    EmailVerification verification = new EmailVerification();
    verification.email = email;
    verification.purpose = purpose;
    verification.code = hashedCode;
    verification.expiresAt = expiresAt;
      return verification;
  }

  public boolean isExpired(LocalDateTime now) {
    return now.isAfter(this.expiresAt);
  }

  public boolean isVerified() {
    return this.verifiedAt != null;
  }

  public void markVerified(LocalDateTime now) {
    this.verifiedAt = now;
  }
}