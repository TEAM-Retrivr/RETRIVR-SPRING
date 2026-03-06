package retrivr.retrivrspring.domain.entity.item;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item")
public class Item extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "rental_duration", nullable = false)
  private Integer rentalDuration;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = true)
  private String guaranteedGoods;

  @Column(nullable = false)
  private boolean useMessageAlarmService;

  @Column(nullable = false)
  private Integer totalQuantity;

  @Column(nullable = false)
  private Integer availableQuantity;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemManagementType itemManagementType;

  @Builder.Default
  @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
  private List<ItemUnit> itemUnits = new ArrayList<>();

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "item")
  List<ItemBorrowerField> itemBorrowerFields = new ArrayList<>();

  /**
   * 현재 대여 가능한 물품인지 확인한다.
   * 비활성 상태가 아니고, 대여 가능 수량이 1개 이상이어야 한다.
   */
  public boolean isRentalAble() {
    return this.isActive && this.availableQuantity > 0;
  }

  /**
   * UNIT 관리 방식인지 확인한다.
   * itemManagementType은 반드시 지정되어 있어야 한다.
   */
  public boolean isUnitType() {
    if (this.itemManagementType == null) {
      throw new DomainException(ErrorCode.INVALID_ITEM, "물건에는 물건 유형이 지정되어 있어야 합니다.");
    }
    return this.itemManagementType.equals(ItemManagementType.UNIT);
  }

  /**
   * 대여 요청 처리.
   *
   * 정책:
   * - 대여 요청 시점에 재고를 선점한다. (availableQuantity 감소)
   * - UNIT 타입이면 ItemUnit 상태도 함께 요청 상태로 전이한다.
   * - NON_UNIT 타입이면 ItemUnit 없이 수량만 감소시킨다.
   */
  public void onRentalRequested(@Nullable ItemUnit itemUnit) {
    // 1. 대여 가능 여부 확인
    validateRentalAble();

    // 2. itemUnit 이 null 일 경우 Non Unit Type 전용 메소드 호축
    if (itemUnit == null) {
      validateNonUnitTypeRequired();
      minusOneAvailableQuantity();
      return;
    }

    validateUnitOperation(itemUnit);

    // 3. ItemUnit 의 대여 요청 처리
    itemUnit.onRentalRequested();
    // 4. 해당 Item 의 수량 감소
    minusOneAvailableQuantity();
  }

  /**
   * 대여 승인 처리.
   *
   * 정책:
   * - 재고는 요청 시점에 이미 선점되었으므로 승인 시에는 수량을 변경하지 않는다.
   * - UNIT 타입이면 ItemUnit 상태만 승인 상태로 전이한다.
   */
  public void onRentalApprove(@Nullable ItemUnit itemUnit) {
    if (itemUnit == null) {
      validateNonUnitTypeRequired();
      return;
    }

    validateUnitOperation(itemUnit);

    itemUnit.onRentalApprove();
  }

  /**
   * 대여 거절 처리.
   *
   * 정책:
   * - 요청 단계에서 선점했던 재고를 복구한다.
   * - UNIT 타입이면 ItemUnit 상태도 함께 되돌린다.
   */
  public void onRentalRejected(@Nullable ItemUnit itemUnit) {
    if (itemUnit == null) {
      validateNonUnitTypeRequired();
      plusOneAvailableQuantity();
      return;
    }

    validateUnitOperation(itemUnit);

    plusOneAvailableQuantity();
    itemUnit.onRentalRejected();
  }

  /**
   * 반납 처리.
   *
   * 정책:
   * - 반납되면 다시 대여 가능한 재고에 포함시킨다.
   * - UNIT 타입이면 ItemUnit도 반납 완료 후 다시 대여 가능 상태로 전이한다.
   */
  public void onRentalReturned(@Nullable ItemUnit itemUnit) {
    if (itemUnit == null) {
      validateNonUnitTypeRequired();
      plusOneAvailableQuantity();
      return;
    }

    validateUnitOperation(itemUnit);

    plusOneAvailableQuantity();
    itemUnit.onRentalReturned();
  }

  /**
   * 대여 가능 수량을 1 증가시킨다.
   * 총 수량보다 커질 수 없다.
   */
  private void plusOneAvailableQuantity() {
    if (this.availableQuantity >= this.totalQuantity) {
      throw new DomainException(ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION);
    }
    availableQuantity++;
  }

  /**
   * 대여 가능 수량을 1 감소시킨다.
   * 0 이하로 내려갈 수 없다.
   */
  private void minusOneAvailableQuantity() {
    if (this.availableQuantity <= 0) {
      throw new DomainException(ErrorCode.AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION);
    }
    availableQuantity--;
  }


  /**
   * 대여자 입력값 검증.
   *
   * 검증 항목:
   * 1. null 여부
   * 2. 정의되지 않은 fieldKey가 포함되었는지
   * 3. 필수값 누락 여부
   * 4. 각 필드 타입이 올바른지
   */
  public void validationItemBorrowerFieldsWith(Map<String, String> values) {
    validateBorrowerFieldMapNotNull(values);
    validateNoUnknownBorrowerFieldKey(values);
    validateRequiredAndType(values);
  }

  /**
   * 대여자 입력값 맵이 null인지 검증한다.
   */
  private void validateBorrowerFieldMapNotNull(Map<String, String> values) {
    if (values == null) {
      throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Borrower fields map is null");
    }
  }

  /**
   * 정의되지 않은 borrower field key가 포함되었는지 검증한다.
   */
  private void validateNoUnknownBorrowerFieldKey(Map<String, String> values) {
    Set<String> allowedKeys = new HashSet<>();
    for (ItemBorrowerField field : itemBorrowerFields) {
      allowedKeys.add(field.getFieldKey());
    }

    for (String key : values.keySet()) {
      if (!allowedKeys.contains(key)) {
        throw new DomainException(
            ErrorCode.ILLEGAL_BORROWER_FIELD,
            "Unknown borrower field key: " + key
        );
      }
    }
  }

  /**
   * 필수값 여부와 타입을 검증한다.
   * optional 필드는 값이 비어 있으면 타입 검증을 생략한다.
   */
  private void validateRequiredAndType(Map<String, String> values) {
    for (ItemBorrowerField field : itemBorrowerFields) {
      String key = field.getFieldKey();
      String raw = values.get(key);

      if (field.isRequired() && isBlank(raw)) {
        throw new DomainException(
            ErrorCode.ILLEGAL_BORROWER_FIELD,
            "Required borrower field missing: " + key
        );
      }

      if (isBlank(raw)) {
        continue;
      }

      field.validateType(raw, key);
    }
  }

  /**
   * 대여 가능 여부를 검증한다.
   */
  private void validateRentalAble() {
    if (!isRentalAble()) {
      throw new DomainException(ErrorCode.NOT_AVAILABLE_ITEM);
    }
  }

  /**
   * UNIT 타입 공통 검증.
   *
   * 검증 항목:
   * - 현재 Item이 UNIT 타입인지
   * - 전달된 ItemUnit이 현재 Item 소속인지
   */
  private void validateUnitOperation(ItemUnit itemUnit) {
    validateUnitTypeRequired();
    validateItemUnitBelongsToThisItem(itemUnit);
  }

  /**
   * itemUnit이 전달된 경우 현재 Item은 반드시 UNIT 타입이어야 한다.
   */
  private void validateUnitTypeRequired() {
    if (!this.isUnitType()) {
      throw new DomainException(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
    }
  }

  /**
   * itemUnit 없이 처리할 수 있는지 검증한다.
   * UNIT 타입은 반드시 특정 ItemUnit이 필요하다.
   */
  private void validateNonUnitTypeRequired() {
    if (this.isUnitType()) {
      throw new DomainException(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
    }
  }

  /**
   * 전달된 ItemUnit이 현재 Item 소속인지 검증한다.
   */
  private void validateItemUnitBelongsToThisItem(ItemUnit itemUnit) {
    if (!itemUnit.isBelongTo(this)) {
      throw new DomainException(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
    }
  }


  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}