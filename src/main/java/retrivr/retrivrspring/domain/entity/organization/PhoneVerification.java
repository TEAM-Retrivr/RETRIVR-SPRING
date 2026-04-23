package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "phone_verification",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_phone_verification_phone_purpose",
            columnNames = {"phone", "purpose"}
        )
    }
)
public class PhoneVerification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Embedded
  private PhoneNumber phone;

  @OneToOne(mappedBy = "phoneVerification", orphanRemoval = true, cascade = CascadeType.ALL)
  private PhoneVerificationToken phoneVerificationToken;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PhoneVerificationPurpose purpose;

  @Column(nullable = false)
  private String codeHash;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  private LocalDateTime verifiedAt;

  @Column(nullable = false)
  private int requestAttempts;

  private LocalDateTime lastAttemptAt;

  private LocalDateTime requestLockExpiration;

  private static final int MAX_REQUEST_ATTEMPTS = 3;
  private static final int LOCK_TIME = 5; // 분

  public static PhoneVerification create(
      PhoneNumber phone,
      PhoneVerificationPurpose purpose,
      String codeHash,
      LocalDateTime now
  ) {
    return PhoneVerification.builder()
        .phone(phone)
        .purpose(purpose)
        .codeHash(codeHash)
        .expiresAt(now.plusMinutes(10))
        .requestAttempts(1)
        .build();
  }

  public void refresh(String codeHash, LocalDateTime now) {
    if (lastAttemptAt != null && lastAttemptAt.isBefore(now.minusMinutes(LOCK_TIME))) {
      unlock();
    }
    if (isLocked(now)) {
      throw new DomainException(ErrorCode.TOO_MANY_PHONE_VERIFICATION_REQUEST);
    }

    this.codeHash = codeHash;
    this.expiresAt = now.plusMinutes(15);
    this.verifiedAt = null;
    this.requestAttempts++;
    lastAttemptAt = now;
    clearToken();

    if (requestAttempts >= 3) {
      lock(now);
    }
  }

  public boolean isLocked(LocalDateTime now) {
    return requestLockExpiration != null && now.isBefore(requestLockExpiration);
  }

  public void lock(LocalDateTime now) {
    this.requestLockExpiration = now.plusMinutes(LOCK_TIME);
    this.requestAttempts = 0;
  }

  private void unlock() {
    this.requestLockExpiration = null;
    this.requestAttempts = 0;
  }

  public boolean isExpired(LocalDateTime now) {
    return !now.isBefore(this.expiresAt);
  }

  public boolean isMatchesPurpose(PhoneVerificationPurpose purpose) {
    return this.purpose == purpose;
  }

  public void markVerified(LocalDateTime now) {
    this.verifiedAt = now;
  }

  public void clearToken() {
    if (this.phoneVerificationToken != null) {
      this.phoneVerificationToken.delete();
      this.phoneVerificationToken = null;
    }
  }
}
