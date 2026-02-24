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

  public void refresh(String hashedCode, LocalDateTime expiresAt) {
    this.code = hashedCode;
    this.expiresAt = expiresAt;
    this.verifiedAt = null; // 새 코드 발급 시 인증 상태 초기화
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