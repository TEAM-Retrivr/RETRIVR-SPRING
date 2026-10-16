package retrivr.retrivrspring.domain.entity.locker.embed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerRentalTerms {

  @Column(nullable = false)
  private int rentalDays;

  @Column(nullable = false)
  private int maxExtensionCount;

  @Column(nullable = false)
  private int extensionDays;

  @Column(nullable = false)
  private long termsVersion;

  @Column(nullable = false)
  private LocalDateTime termsAgreedAt;

  public static LockerRentalTerms create(int rentalDays, int maxExtensionCount, int extensionDays, long termsVersion, LocalDateTime termsAgreedAt) {
    LockerRentalTerms instance = LockerRentalTerms.builder()
        .rentalDays(rentalDays)
        .maxExtensionCount(maxExtensionCount)
        .extensionDays(extensionDays)
        .termsVersion(termsVersion)
        .termsAgreedAt(termsAgreedAt)
        .build();
    instance.validate();
    return instance;
  }

  public void validate() {
    validateRentalDays(this.rentalDays);
    validateMaxExtensionCount(this.maxExtensionCount);
    validateExtensionDays(this.extensionDays);
    validateTermsAgreedAt(this.termsAgreedAt);
  }

  private void validateRentalDays(int rentalDays) {
    if (rentalDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여 일수는 0보다 커야 합니다.");
    }
  }

  private void validateMaxExtensionCount(int maxExtensionCount) {
    if (maxExtensionCount < 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "최대 연장 횟수는 0 이상이어야 합니다.");
    }
  }

  private void validateExtensionDays(int extensionDays) {
    if (extensionDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "연장 일수는 0보다 커야 합니다.");
    }
  }

  private void validateTermsAgreedAt(LocalDateTime termsAgreedAt) {
    if (termsAgreedAt == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "약관 동의 시각은 필수입니다.");
    }
  }
}
