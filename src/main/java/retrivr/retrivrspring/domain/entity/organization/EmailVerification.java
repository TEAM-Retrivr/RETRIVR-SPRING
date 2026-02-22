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
@Table(name = "email_verification")
public class EmailVerification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "email_verification_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  public static EmailVerification create(
          Organization organization,
          String code,
          LocalDateTime expiresAt
  ) {
    EmailVerification verification = new EmailVerification();
    verification.organization = organization;
    verification.code = code;
    verification.expiresAt = expiresAt;
    verification.verifiedAt = null;
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