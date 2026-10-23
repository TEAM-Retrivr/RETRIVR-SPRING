package retrivr.retrivrspring.domain.entity.locker;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Table(
    name = "locker_zone",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_locker_zone_name",
            columnNames = {"organization_id", "name"}
        ),
        @UniqueConstraint(
            name = "uk_locker_zone_sort_order",
            columnNames = {"organization_id", "sortOrder"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerZone extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "zone", orphanRemoval = true,
      cascade = {CascadeType.PERSIST})
  private List<Locker> lockers;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private int rowCount;

  @Column(nullable = false)
  private int columnCount;

  @Column(nullable = false)
  private int maxLockerCount;

  @Column(nullable = false)
  private int availableLockerCount;

  @Version
  private Long version;

  // 배치 변경 시 zone의 낙관적 잠금 버전도 갱신하고, 초기화 전후 사물함을 구분한다.
  @Column(nullable = false)
  private long layoutRevision;

  public static int MAX_ZONE_COUNT = 20;

  public static LockerZone create(Organization organization, String name, int sortOrder, int rowCount, int columnCount) {
    LockerZone instance = LockerZone.builder()
        .organization(organization)
        .name(name)
        .sortOrder(sortOrder)
        .rowCount(rowCount)
        .columnCount(columnCount)
        .maxLockerCount(rowCount * columnCount)
        .availableLockerCount(rowCount * columnCount)
        .build();
    instance.createLockerLayout();
    instance.validateForCreation();
    return instance;
  }

  public void createLockerLayout() {
    List<Locker> lockers = new ArrayList<>(this.rowCount * this.columnCount);
    int label = 1;

    for (int rowIndex = 0; rowIndex < this.rowCount; rowIndex++) {
      for (int columnIndex = 0; columnIndex < this.columnCount; columnIndex++) {
        lockers.add(Locker.create(Integer.toString(label++), rowIndex, columnIndex, this));
      }
    }
    this.lockers = lockers;
  }

  public void updateName(String name) {
    validateName(name);
    this.name = name;
  }

  public void updateSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public void updateSize(int rowCount, int columnCount) {
    validateRowCount(rowCount);
    validateColumnCount(columnCount);
    this.rowCount = rowCount;
    this.columnCount = columnCount;
    this.maxLockerCount = rowCount * columnCount;
    this.availableLockerCount = rowCount * columnCount;
    markLayoutChanged();
  }

  public void validateVersion(Long expectedVersion) {
    if (expectedVersion == null || !expectedVersion.equals(this.version)) {
      throw new DomainException(ErrorCode.VERSION_CONFLICT);
    }
  }

  public void markLayoutChanged() {
    this.layoutRevision++;
  }

  public void removeLocker(Locker locker) {
    this.lockers.remove(locker);
    this.maxLockerCount--;
    this.availableLockerCount--;
    markLayoutChanged();
  }

  public void changeEnabledLockerCount(int difference) {
    this.availableLockerCount += difference;
    this.maxLockerCount += difference;
    markLayoutChanged();
  }

  public void changeLockerCount(int difference) {
    this.availableLockerCount += difference;
  }

  public boolean isValidRowIndex(int rowIndex) {
    return rowIndex >= 0 && rowIndex < this.rowCount;
  }

  public boolean isValidColumnIndex(int columnIndex) {
    return columnIndex >= 0 && columnIndex < this.columnCount;
  }

  /**
   * Column Validation Method
   * 결정사항: 사물함 구역 이름 최대 글자 수.
   */
  private void validateForCreation() {
    validateOrganization(this.organization);
    validateName(this.name);
    validateRowCount(this.rowCount);
    validateColumnCount(this.columnCount);
  }

  private void validateOrganization(Organization organization) {
    if (organization == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_ZONE_VALUE, "사물함 구역의 단체는 필수입니다.");
    }
  }

  private void validateName(String name) {
    if (name == null || name.isBlank() || name.codePointCount(0, name.length()) > 20) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_ZONE_VALUE, "사물함 구역 이름은 공백이 아닌 20자 이하여야 합니다.");
    }
  }

  private void validateRowCount(int rowCount) {
    if (rowCount <= 0 || rowCount > 30) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_ZONE_VALUE, "사물함 구역의 행 개수는 0보다 크고 30이하여야 합니다.");
    }
  }

  private void validateColumnCount(int columnCount) {
    if (columnCount <= 0 || columnCount > 300) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_ZONE_VALUE, "사물함 구역의 열 개수는 0보다 크고 300이하여야 합니다.");
    }
  }
}
