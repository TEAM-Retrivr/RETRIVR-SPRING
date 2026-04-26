package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "admin_code_verification_token",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_admin_code_verification_token_org_purpose",
            columnNames = {"organization_id", "purpose"}
        ),
        @UniqueConstraint(
            name = "uk_admin_code_verification_token_hash",
            columnNames = {"token_hash"}
        )
    }
)
public class AdminCodeVerificationToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false)
  private AdminCodeVerificationPurpose purpose;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  public void refresh(String tokenHash, LocalDateTime now) {
    this.tokenHash = tokenHash;
    this.expiresAt = now.plusHours(1);
    this.usedAt = null;
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt.isBefore(now);
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed(LocalDateTime now) {
    this.usedAt = now;
  }

  public static AdminCodeVerificationToken create(Organization organization, String tokenHash, AdminCodeVerificationPurpose purpose, LocalDateTime now) {
    return AdminCodeVerificationToken.builder()
        .organization(organization)
        .tokenHash(tokenHash)
        .purpose(purpose)
        .expiresAt(now.plusHours(1))
        .build();
  }
}
