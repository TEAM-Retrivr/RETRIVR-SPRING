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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerActivationStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerOperationStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Table(
    name = "locker",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_locker_label",
            columnNames = {"zone_id", "label"}
        ),
        @UniqueConstraint(
            name = "uk_locker_index",
            columnNames = {"zone_id", "rowIndex", "columnIndex"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Locker extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "zone_id", nullable = false)
  private LockerZone zone;

  @Column(nullable = false, length = 50)
  private String label;

  @Column(nullable = false)
  private int rowIndex;

  @Column(nullable = false)
  private int columnIndex;

  @Column(nullable = false)
  @Builder.Default
  private int rowSpan = 0; // 행 병합 칸수

  @Column(nullable = false)
  @Builder.Default
  private int columnSpan = 0; // 열 병합 칸수

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerActivationStatus activationStatus = LockerActivationStatus.ENABLED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerOperationStatus operationStatus = LockerOperationStatus.AVAILABLE;

  public static Locker create(String label, int rowIndex, int columnIndex, LockerZone zone) {
    Locker instance = Locker.builder()
        .label(label)
        .rowIndex(rowIndex)
        .columnIndex(columnIndex)
        .zone(zone)
        .build();
    instance.validateForCreation();
    return instance;
  }

  /**
   * Column Validate Method
   */
  private void validateForCreation() {
    validateZone(this.zone);
    validateLabel(this.label);
    validateRowIndex(this.zone, this.rowIndex);
    validateColumnIndex(this.zone, this.columnIndex);
  }

  private void validateZone(LockerZone zone) {
    if (zone == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_VALUE, "사물함 구역은 필수입니다.");
    }
  }

  private void validateLabel(String label) {
    if (label == null || label.isBlank() || label.codePointCount(0, label.length()) > 50) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_VALUE, "사물함 라벨은 공백이 아닌 50자 이하여야 합니다.");
    }
  }

  private void validateRowIndex(LockerZone zone, int rowIndex) {
    if (!zone.isValidRowIndex(rowIndex)) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_VALUE, "사물함 행 인덱스는 0 이상이고 사물함 구역 행 크기보다 작아야 합니다.");
    }
  }

  private void validateColumnIndex(LockerZone zone, int columnIndex) {
    if (!zone.isValidColumnIndex(columnIndex)) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_VALUE, "사물함 열 인덱스는 0 이상이고 사물함 구역 열 크기보다 작아야 합니다.");
    }
  }
}
