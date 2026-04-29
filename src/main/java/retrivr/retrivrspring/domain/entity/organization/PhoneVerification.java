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

  @Column(nullable = false)
  private int failedAttempts;

  private LocalDateTime lastAttemptAt;

  private LocalDateTime requestLockExpiration;

  private LocalDateTime verificationLockExpiration;

  private static final int MAX_REQUEST_ATTEMPTS = 3;
  public static final int MAX_FAILED_VERIFICATION_ATTEMPTS = 20;
  public static final int EXPIRATION_TIME_MINUTES = 3;
  private static final int REQUEST_LOCK_TIME_MINUTES = 5;
  private static final int VERIFICATION_LOCK_TIME_MINUTES = 5;

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
        .expiresAt(now.plusMinutes(EXPIRATION_TIME_MINUTES))
        .requestAttempts(1)
        .failedAttempts(0)
        .build();
  }

  public void refresh(String codeHash, LocalDateTime now) {
    if (lastAttemptAt != null
        && lastAttemptAt.isBefore(now.minusMinutes(REQUEST_LOCK_TIME_MINUTES))) {
      unlockRequest();
    }
    if (isRequestLocked(now)) {
      throw new DomainException(ErrorCode.TOO_MANY_PHONE_VERIFICATION_REQUEST);
    }

    this.codeHash = codeHash;
    this.expiresAt = now.plusMinutes(EXPIRATION_TIME_MINUTES);
    this.verifiedAt = null;
    this.failedAttempts = 0;
    this.verificationLockExpiration = null;
    this.requestAttempts++;
    this.lastAttemptAt = now;
    clearToken();

    if (requestAttempts >= MAX_REQUEST_ATTEMPTS) {
      lockRequest(now);
    }
  }

  public boolean isRequestLocked(LocalDateTime now) {
    return requestLockExpiration != null && now.isBefore(requestLockExpiration);
  }

  public void lockRequest(LocalDateTime now) {
    this.requestLockExpiration = now.plusMinutes(REQUEST_LOCK_TIME_MINUTES);
    this.requestAttempts = 0;
  }

  private void unlockRequest() {
    this.requestLockExpiration = null;
    this.requestAttempts = 0;
  }

  public boolean isVerificationLocked(LocalDateTime now) {
    return verificationLockExpiration != null && now.isBefore(verificationLockExpiration);
  }

  public int incrementFailedAttempts() {
    this.failedAttempts += 1;
    return this.failedAttempts;
  }

  public void lockVerification(LocalDateTime now) {
    this.verificationLockExpiration = now.plusMinutes(VERIFICATION_LOCK_TIME_MINUTES);
  }

  public boolean isExpired(LocalDateTime now) {
    return !now.isBefore(this.expiresAt);
  }

  public boolean isMatchesPurpose(PhoneVerificationPurpose purpose) {
    return this.purpose == purpose;
  }

  public void markVerified(LocalDateTime now) {
    this.verifiedAt = now;
    this.failedAttempts = 0;
    this.verificationLockExpiration = null;
  }

  public void clearToken() {
    if (this.phoneVerificationToken != null) {
      this.phoneVerificationToken.delete();
      this.phoneVerificationToken = null;
    }
  }
}
