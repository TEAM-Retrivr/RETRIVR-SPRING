package retrivr.retrivrspring.domain.entity.locker;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.locker.embed.LockerBorrowerInfo;
import retrivr.retrivrspring.domain.entity.locker.embed.LockerRentalTerms;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Table(
    name = "locker_rental",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "locker_rental_public_id"
            , columnNames = {"public_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerRental extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 64)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "locker_id")
  private Locker locker;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerRentalStatus status = LockerRentalStatus.REQUESTED;

  @Embedded
  private LockerBorrowerInfo borrower;

  @Column(nullable = false)
  private LocalDateTime requestedAt;

  private LocalDateTime approvedAt;

  private String approvedBy;

  private LocalDateTime rejectedAt;

  private String rejectedBy;

  private String rejectReason;

  private LocalDate startDate;

  private LocalDate dueDate;

  private LocalDateTime returnedAt;

  private String returnedBy;

  @Column(nullable = false)
  private int extensionCount;

  @Embedded
  private LockerRentalTerms rentalTerms;

  @Column(nullable = false)
  private String zoneNameSnapshot;

  @Column(nullable = false)
  private String lockerLabelSnapshot;

  @Column(nullable = false)
  private int lockerRowIndexSnapshot;

  @Column(nullable = false)
  private int lockerColumnIndexSnapshot;

  public static LockerRental create(String publicId, Organization organization, Locker locker, LockerBorrowerInfo borrower,
      LockerRentalTerms rentalTerms, LocalDateTime requestedAt) {
    LockerRental instance = LockerRental.builder()
        .publicId(publicId)
        .organization(organization)
        .locker(locker)
        .borrower(borrower)
        .requestedAt(requestedAt)
        .extensionCount(0)
        .rentalTerms(rentalTerms)
        .build();
    instance.saveSnapshot(locker);
    instance.validateForCreation();
    return instance;
  }

  private void saveSnapshot(Locker locker) {
    validateLocker(locker);
    this.zoneNameSnapshot = locker.getZone().getName();
    this.lockerLabelSnapshot = locker.getLabel();
    this.lockerRowIndexSnapshot = locker.getRowIndex();
    this.lockerColumnIndexSnapshot = locker.getColumnIndex();
  }

  /**
   * Column Validate Method
   */
  private void validateForCreation() {
    validatePublicId(this.publicId);
    validateOrganization(this.organization);
    validateLocker(this.locker);
    validateBorrower(this.borrower);
    validateRentalTerms(this.rentalTerms);
    validateRequestedAt(this.requestedAt);
    validateZoneNameSnapshot(this.zoneNameSnapshot);
    validateLockerLabelSnapshot(this.lockerLabelSnapshot);
    validateLockerRowIndexSnapshot(this.lockerRowIndexSnapshot);
    validateLockerColumnIndexSnapshot(this.lockerColumnIndexSnapshot);
  }

  private void validateRentalDates(LocalDate startDate, LocalDate dueDate) {
    if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "반납 예정일은 대여 시작일보다 빠를 수 없습니다.");
    }
  }

  private void validatePublicId(String publicId) {
    if (publicId == null || publicId.isBlank() || publicId.codePointCount(0, publicId.length()) > 64) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "공개 ID는 공백이 아닌 64자 이하여야 합니다.");
    }
  }

  private void validateOrganization(Organization organization) {
    if (organization == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "단체는 필수입니다.");
    }
  }

  private void validateLocker(Locker locker) {
    if (locker == null || locker.getZone() == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "사물함과 사물함 구역은 필수입니다.");
    }
  }

  private void validateStatus(LockerRentalStatus status) {
    if (status == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여 상태는 필수입니다.");
    }
  }

  private void validateBorrower(LockerBorrowerInfo borrower) {
    if (borrower == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여자 정보는 필수입니다.");
    }
    borrower.validate();
  }

  private void validateRequestedAt(LocalDateTime requestedAt) {
    if (requestedAt == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "요청 시각은 필수입니다.");
    }
  }

  private void validateExtensionCount(int extensionCount) {
    if (extensionCount < 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "연장 횟수는 0 이상이어야 합니다.");
    }
  }

  private void validateRentalTerms(LockerRentalTerms rentalTerms) {
    if (rentalTerms == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여 조건은 필수입니다.");
    }
    rentalTerms.validate();
  }

  private void validateZoneNameSnapshot(String zoneNameSnapshot) {
    if (zoneNameSnapshot == null || zoneNameSnapshot.isBlank() || zoneNameSnapshot.codePointCount(0, zoneNameSnapshot.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "zoneNameSnapshot 값은 공백이 아닌 255자 이하여야 합니다.");
    }
  }

  private void validateLockerLabelSnapshot(String lockerLabelSnapshot) {
    if (lockerLabelSnapshot == null || lockerLabelSnapshot.isBlank() || lockerLabelSnapshot.codePointCount(0, lockerLabelSnapshot.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "lockerLabelSnapshot 값은 공백이 아닌 255자 이하여야 합니다.");
    }
  }

  private void validateLockerRowIndexSnapshot(int lockerRowIndexSnapshot) {
    if (lockerRowIndexSnapshot < 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "lockerRowIndexSnapshot 값은 0 이상이어야 합니다.");
    }
  }

  private void validateLockerColumnIndexSnapshot(int lockerColumnIndexSnapshot) {
    if (lockerColumnIndexSnapshot < 0) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "lockerColumnIndexSnapshot 값은 0 이상이어야 합니다.");
    }
  }

  private void validateApprovedBy(String approvedBy) {
    if (approvedBy != null && approvedBy.codePointCount(0, approvedBy.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "approvedBy 값은 255자 이하여야 합니다.");
    }
  }

  private void validateRejectedBy(String rejectedBy) {
    if (rejectedBy != null && rejectedBy.codePointCount(0, rejectedBy.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "rejectedBy 값은 255자 이하여야 합니다.");
    }
  }

  private void validateRejectReason(String rejectReason) {
    if (rejectReason != null && rejectReason.codePointCount(0, rejectReason.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "rejectReason 값은 255자 이하여야 합니다.");
    }
  }

  private void validateReturnedBy(String returnedBy) {
    if (returnedBy != null && returnedBy.codePointCount(0, returnedBy.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "returnedBy 값은 255자 이하여야 합니다.");
    }
  }

}
