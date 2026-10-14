package retrivr.retrivrspring.domain.entity.locker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.organization.Organization;

@Entity
@Table(name = "locker_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerPolicy extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false, unique = true)
  private Organization organization;

  @Column(nullable = false)
  private int rentalDays;

  @Column(nullable = false)
  private int maxExtensionCount;

  @Column(nullable = false)
  private int extensionDays;

  @Column(nullable = false, columnDefinition = "text")
  private String terms;

  @Column(nullable = false)
  @Builder.Default
  private Long termsVersion = 1L;

  @Version
  private Long version;

  public static LockerPolicy create(Organization organization, int rentalDays, int maxExtensionCount, int extensionDays, String terms) {
    LockerPolicy instance = LockerPolicy.builder()
        .organization(organization)
        .rentalDays(rentalDays)
        .maxExtensionCount(maxExtensionCount)
        .extensionDays(extensionDays)
        .terms(terms)
        .build();
    instance.validateForCreation();
    return instance;
  }

  /**
   * Column Validate Method
   */
  private void validateForCreation() {
    validateOrganization(this.organization);
    validateRentalDays(this.rentalDays);
    validateMaxExtensionCount(this.maxExtensionCount);
    validateExtensionDays(this.extensionDays);
    validateTerms(this.terms);
  }

  private void validateOrganization(Organization organization) {
    if (organization == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_POLICY_VALUE, "단체는 필수입니다.");
    }
  }

  private void validateRentalDays(int rentalDays) {
    if (rentalDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_POLICY_VALUE, "대여 일수는 0보다 커야 합니다.");
    }
  }

  private void validateMaxExtensionCount(int maxExtensionCount) {
    if (maxExtensionCount < 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_POLICY_VALUE, "최대 연장 횟수는 0 이상이어야 합니다.");
    }
  }

  private void validateExtensionDays(int extensionDays) {
    if (extensionDays <= 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_POLICY_VALUE, "연장 일수는 0보다 커야 합니다.");
    }
  }

  private void validateTerms(String terms) {
    if (terms == null || terms.isBlank()) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_POLICY_VALUE, "약관은 필수입니다.");
    }
  }
}
