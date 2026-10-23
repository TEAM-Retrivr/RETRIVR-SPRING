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
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerEnableStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerOperationStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Table(
    name = "locker",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_locker_label",
            columnNames = {"zone_id", "layout_revision", "label"}
        ),
        @UniqueConstraint(
            name = "uk_locker_index",
            columnNames = {"zone_id", "layout_revision", "row_index", "column_index"}
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parent_locker", nullable = false)
  private Locker parentLocker;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerEnableStatus enableStatus = LockerEnableStatus.ENABLED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LockerOperationStatus operationStatus = LockerOperationStatus.AVAILABLE;

  @Version
  private Long version;

  @Column(name = "layout_revision", nullable = false)
  private long layoutRevision;

  public static Locker create(String label, int rowIndex, int columnIndex, LockerZone zone) {
    Locker instance = Locker.builder()
        .label(label)
        .rowIndex(rowIndex)
        .columnIndex(columnIndex)
        .zone(zone)
        .layoutRevision(zone == null ? 0 : zone.getLayoutRevision())
        .build();
    instance.parentLocker = instance;
    instance.validateForCreation();
    return instance;
  }

  public void enable() {
    if (isEnabled()) {
      throw new DomainException(ErrorCode.LOCKER_ALREADY_ENABLED);
    }
    if (isCombinedLocker()) {
      throw new DomainException(ErrorCode.LOCKER_COMBINED_ENABLE_NOT_ALLOWED);
    }
    this.enableStatus = LockerEnableStatus.ENABLED;
    this.zone.changeEnabledLockerCount(1);
  }

  public void disable() {
    if (!isEnabled()) {
      throw new DomainException(ErrorCode.LOCKER_ALREADY_DISABLED);
    }
    if (this.operationStatus != LockerOperationStatus.AVAILABLE) {
      throw new DomainException(ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS);
    }
    this.enableStatus = LockerEnableStatus.DISABLED;
    this.zone.changeEnabledLockerCount(-1);
  }

  public void enableRental() {
    if (!isEnabled()) {
      throw new DomainException(ErrorCode.LOCKER_DISABLED);
    }
    if (this.operationStatus == LockerOperationStatus.PENDING
        || this.operationStatus == LockerOperationStatus.RENTED) {
      throw new DomainException(ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS);
    }
    if (this.operationStatus != LockerOperationStatus.UNAVAILABLE) {
      throw new DomainException(ErrorCode.LOCKER_RENTAL_ENABLE_NOT_ALLOWED);
    }
    this.zone.changeLockerCount(1);
    this.operationStatus = LockerOperationStatus.AVAILABLE;
  }

  public void disableRental() {
    if (!isEnabled()) {
      throw new DomainException(ErrorCode.LOCKER_DISABLED);
    }
    if (!isRentalAble()) {
      throw new DomainException(ErrorCode.LOCKER_RENTAL_ENABLE_NOT_ALLOWED);
    }
    this.zone.changeLockerCount(-1);
    this.operationStatus = LockerOperationStatus.UNAVAILABLE;
  }

  public boolean isEnabled() {
    return this.enableStatus == LockerEnableStatus.ENABLED;
  }

  public boolean isRentalAble() {
    return this.enableStatus == LockerEnableStatus.ENABLED && this.operationStatus == LockerOperationStatus.AVAILABLE;
  }

  public void updateLabel(String label) {
    validateLabel(label);
    this.label = label;
  }

  public void validateOwner(Organization  organization) {
    if (!Objects.equals(this.zone.getOrganization().getId(), organization.getId())) {
      throw new DomainException(ErrorCode.LOCKER_OWNER_MISMATCH);
    }
  }

  public void validateEnable() {
    if (this.enableStatus == LockerEnableStatus.DISABLED) {
      throw new DomainException(ErrorCode.LOCKER_DISABLED);
    }
  }

  public void combine(Locker parent) {
    if (!parent.isRepresentativeLocker()) {
      throw new DomainException(ErrorCode.LOCKER_NOT_REPRESENTATIVE);
    }
    this.parentLocker = parent;
    this.disable();
  }

  public boolean isRepresentativeLocker() {
    return this.enableStatus == LockerEnableStatus.ENABLED
        && !hasParent();
  }

  public void validateRepresentativeLocker() {
    if (!isRepresentativeLocker()) {
      throw new DomainException(ErrorCode.LOCKER_NOT_REPRESENTATIVE);
    }
  }
  public boolean isCombinedLocker() {
    return this.enableStatus == LockerEnableStatus.DISABLED
        && hasParent();
  }

  public boolean hasParent() {
    return !Objects.equals(getParentLocker().getId(), getId());
  }

  public boolean isChildOf(Locker parent) {
    return Objects.equals(getParentLocker().getId(), parent.getId());
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
