package retrivr.retrivrspring.domain.entity.locker;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerExtensionStatus;

@Entity
@Table(
    name = "locker_rental_extension",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rental_extension_sequence",
            columnNames = {"rental_id", "sequence"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerRentalExtension extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rental_id", nullable = false)
  private LockerRental rental;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerExtensionStatus status = LockerExtensionStatus.REQUESTED;

  @Column(nullable = false)
  private LocalDateTime requestedAt;

  private LocalDateTime approvedAt;

  private String approvedBy;

  private LocalDateTime rejectedAt;

  private String rejectedBy;

  private String rejectReason;

  @Column(nullable = false)
  private LocalDate previousDueDate;

  @Column(nullable = false)
  private LocalDate newDueDate;

  @Column(nullable = false)
  private long sequence;

  public static LockerRentalExtension create(LockerRental rental, LocalDateTime now, LocalDate previousDueDate, LocalDate newDueDate, long sequence) {
    LockerRentalExtension instance = LockerRentalExtension.builder()
        .rental(rental)
        .requestedAt(now)
        .previousDueDate(previousDueDate)
        .newDueDate(newDueDate)
        .sequence(sequence)
        .build();
    instance.validateForCreation();
    return instance;
  }

  /**
   * Column Validate Method
   */
  private void validateForCreation() {
    validateRental(this.rental);
    validateRequestedAt(this.requestedAt);
    validatePreviousDueDate(this.previousDueDate);
    validateNewDueDate(this.newDueDate);
    validateDueDateRange(this.newDueDate, this.previousDueDate);
  }

  private void validateRental(LockerRental rental) {
    if (rental == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "대여 정보는 필수입니다.");
    }
  }

  private void validateRequestedAt(LocalDateTime requestedAt) {
    if (requestedAt == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "연장 요청 시각은 필수입니다.");
    }
  }

  private void validatePreviousDueDate(LocalDate previousDueDate) {
    if (previousDueDate == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "기존 반납일은 필수입니다.");
    }
  }

  private void validateNewDueDate(LocalDate newDueDate) {
    if (newDueDate == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "새 반납일은 필수입니다.");
    }
  }

  private void validateApprovedBy(String approvedBy) {
    if (approvedBy != null && approvedBy.codePointCount(0, approvedBy.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "approvedBy 값은 255자 이하여야 합니다.");
    }
  }

  private void validateRejectedBy(String rejectedBy) {
    if (rejectedBy != null && rejectedBy.codePointCount(0, rejectedBy.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "rejectedBy 값은 255자 이하여야 합니다.");
    }
  }

  private void validateRejectReason(String rejectReason) {
    if (rejectReason != null && rejectReason.codePointCount(0, rejectReason.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "rejectReason 값은 255자 이하여야 합니다.");
    }
  }

  private void validateDueDateRange(LocalDate newDueDate, LocalDate previousDueDate) {
    if (!newDueDate.isAfter(previousDueDate)) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_EXTENSION_VALUE, "새 반납일은 기존 반납일보다 늦어야 합니다.");
    }
  }

}
