package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;

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

  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EmailVerificationPurpose purpose;

  @Column(nullable = false)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

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
    verification.failedAttempts = 0;
    return verification;
  }

  public void refresh(String hashedCode, LocalDateTime expiresAt) {
    this.code = hashedCode;
    this.expiresAt = expiresAt;
    this.failedAttempts = 0;
    this.verifiedAt = null;
  }

  public boolean isExpired(LocalDateTime now) {
    return !now.isBefore(this.expiresAt);
  }

  public boolean isVerified() {
    return this.verifiedAt != null;
  }

  public void markVerified(LocalDateTime now) {
    this.verifiedAt = now;
    this.failedAttempts = 0;
  }

  public int increaseFailedAttempts() {
    this.failedAttempts += 1;
    return this.failedAttempts;
  }

  public void expire(LocalDateTime now) {
    this.expiresAt = now;
  }
}

