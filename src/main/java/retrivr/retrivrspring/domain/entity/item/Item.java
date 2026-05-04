package retrivr.retrivrspring.domain.entity.item;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

import java.util.*;
import java.util.stream.Collectors;

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

  @Column(nullable = false, unique = true)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private Integer availableQuantity;

  @Column(nullable = false)
  private Integer totalQuantity;

  @Column(name = "rental_duration", nullable = false)
  private Integer rentalDuration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemManagementType itemManagementType;

  @Column(nullable = true)
  private String guaranteedGoods;

  @Column(nullable = false)
  private boolean useMessageAlarmService;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

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
    if (values == null || values.isEmpty()) {
      return;
    }
    validateBorrowerFieldMapNotNull(values);
    validateNoUnknownBorrowerLabel(values);
    validateRequired(values);
  }

  /**
   * 대여자 입력값 맵이 null인지 검증한다.
   */
  private void validateBorrowerFieldMapNotNull(Map<String, String> values) {
    if (values == null) {
      throw new DomainException(ErrorCode.ILLEGAL_BORROWER_LABEL, "Borrower fields map is null");
    }
  }

  /**
   * 정의되지 않은 borrower field key가 포함되었는지 검증한다.
   */
  private void validateNoUnknownBorrowerLabel(Map<String, String> values) {
    Set<String> allowedKeys = new HashSet<>();
    for (ItemBorrowerField field : itemBorrowerFields) {
      allowedKeys.add(field.getLabel());
    }

    for (String label : values.keySet()) {
      if (!allowedKeys.contains(label)) {
        throw new DomainException(
            ErrorCode.ILLEGAL_BORROWER_LABEL,
            "Unknown borrower label : " + label
        );
      }
    }
  }

  /**
   * 필수값 여부와 타입을 검증한다.
   * optional 필드는 값이 비어 있으면 타입 검증을 생략한다.
   */
  private void validateRequired(Map<String, String> values) {
    for (ItemBorrowerField itemBorrowerField : itemBorrowerFields) {
      String label = itemBorrowerField.getLabel();
      String value = values.get(label);

      if (itemBorrowerField.isRequired() && isBlank(value)) {
        throw new DomainException(
            ErrorCode.ILLEGAL_BORROWER_LABEL,
            "Required borrower label missing: " + label
        );
      }
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


  public void overwriteAdmin(String name, String description, Integer rentalDuration,
      Integer totalQuantity, ItemManagementType itemManagementType,
      Boolean useMessageAlarmService, String guaranteedGoods, Boolean isActive) {
    this.name = name;
    this.description = description;
    this.rentalDuration = rentalDuration;
    this.totalQuantity = totalQuantity;
    this.itemManagementType = itemManagementType;
    this.useMessageAlarmService = useMessageAlarmService;
    this.guaranteedGoods = guaranteedGoods;
    this.isActive = isActive;
  }

  public void addAvailableUnitQuantity() {
    this.availableQuantity++;
  }

  public void removeAvailableUnitQuantity() {
    if (this.availableQuantity <= 0) {
      throw new DomainException(ErrorCode.AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION);
    }
    this.availableQuantity--;
  }

  public ItemUnit createUnit(String label) {
    validateUnitTypeRequired();

    ItemUnit itemUnit = ItemUnit.create(this, label);
    this.itemUnits.add(itemUnit);
    return itemUnit;
  }

  public List<ItemUnit> createUnits(List<String> unitLabels) {
    if (!isUnitType() || unitLabels == null || unitLabels.isEmpty()) {
      return List.of();
    }

    List<ItemUnit> createdItemUnits = new ArrayList<>();
    Set<String> seenLabels = new HashSet<>();
    for (String unitLabel : unitLabels) {
      if (!seenLabels.add(unitLabel)) {
        throw new DomainException(
            ErrorCode.BAD_REQUEST_EXCEPTION,
            "Duplicated item unit label."
        );
      }
      createdItemUnits.add(createUnit(unitLabel));
    }
    return createdItemUnits;
  }

  public void renameUnits(List<ItemUnit> itemUnits, List<String> labels) {
    if (itemUnits.size() != labels.size()) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Item unit rename inputs must have the same size."
      );
    }

    for (int i = 0; i < itemUnits.size(); i++) {
      ItemUnit itemUnit = itemUnits.get(i);
      validateItemUnitBelongsToThisItem(itemUnit);
      itemUnit.rename(labels.get(i));
    }
  }

  public void validateUnitChangesForTargetType(
      ItemManagementType targetItemManagementType,
      int createCount,
      int renameCount
  ) {
    if (targetItemManagementType == ItemManagementType.UNIT) {
      return;
    }

    if (createCount > 0 || renameCount > 0) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Non-unit item 의 경우 유닛을 편집할 수 없습니다."
      );
    }
  }

  /**
   * 수정 요청에서 삭제 가능한 기존 유닛 목록을 계산한다.
   * 정책:
   * - UNIT 타입에서만 유닛 삭제가 가능하다.
   * - 삭제 대상은 요청에 포함된 label 기준으로 결정한다.
   * - 대여 중/대여 요청 중 유닛은 삭제할 수 없다.
   */
  public List<String> resolveDeleteUnitLabelsForTargetType(
      ItemManagementType targetItemManagementType,
      List<ItemUnit> currentItemUnits,
      List<String> requestedDeleteUnitLabels
  ) {
    if (this.itemManagementType != ItemManagementType.UNIT
        || targetItemManagementType != ItemManagementType.NON_UNIT) {
      return requestedDeleteUnitLabels;
    }

    return currentItemUnits.stream()
        .map(ItemUnit::getLabel)
        .collect(Collectors.toList());
  }

  public List<ItemUnit> getDeletableUnits(List<ItemUnit> currentItemUnits, List<String> deleteUnitLabels) {
    if (!isUnitType() || deleteUnitLabels == null || deleteUnitLabels.isEmpty()) {
      return List.of();
    }

    Set<String> requestedDeleteLabels = new HashSet<>(deleteUnitLabels);
    if (requestedDeleteLabels.size() != deleteUnitLabels.size()) {
      throw new DomainException(ErrorCode.BAD_REQUEST_EXCEPTION, "중복된 삭제 유닛 label 은 허용되지 않습니다.");
    }
    if (requestedDeleteLabels.size() > currentItemUnits.size()) {
      throw new DomainException(ErrorCode.BAD_REQUEST_EXCEPTION, "삭제할 유닛 수가 현재 유닛 수보다 많습니다.");
    }

    List<ItemUnit> deletedItemUnits = currentItemUnits.stream()
        .filter(itemUnit -> itemUnit.hasLabelIn(requestedDeleteLabels))
        .toList();
    if (deletedItemUnits.size() != requestedDeleteLabels.size()) {
      throw new DomainException(ErrorCode.BAD_REQUEST_EXCEPTION, "삭제 대상 유닛 label 이 존재하지 않습니다.");
    }

    for (ItemUnit deletedItemUnit : deletedItemUnits) {
      deletedItemUnit.validateDeletable();
    }
    return deletedItemUnits;
  }

  /**
   * 기존 유닛 삭제와 새 유닛 추가가 반영된 뒤,
   * 최종 수량과 대여 가능 수량을 한 번에 정리한다.
   */
  public void applyUnitChange(
      ItemManagementType previousItemManagementType,
      Integer previousTotalQuantity,
      List<ItemUnit> currentItemUnits,
      List<ItemUnit> deletedItemUnits,
      List<ItemUnit> createdItemUnits,
      Integer requestedTotalQuantity
  ) {
    int unavailableQuantity = previousTotalQuantity - this.availableQuantity;

    if (previousItemManagementType == this.itemManagementType) {
      if (isUnitType()) {
        validateUnitQuantityMatches(
            requestedTotalQuantity,
            currentItemUnits,
            deletedItemUnits,
            createdItemUnits
        );
        applyAvailableQuantityDelta(deletedItemUnits, createdItemUnits);
        return;
      }

      validateNonUnitHasNoRemainingUnits(currentItemUnits, deletedItemUnits, createdItemUnits);
      syncNonUnitAvailableQuantity(unavailableQuantity, requestedTotalQuantity);
      return;
    }

    if (previousItemManagementType == ItemManagementType.NON_UNIT) {
      validateNonUnitToUnitChange(unavailableQuantity);
      validateUnitQuantityMatches(
          requestedTotalQuantity,
          currentItemUnits,
          deletedItemUnits,
          createdItemUnits
      );
      this.availableQuantity = createdItemUnits.size();
      return;
    }

    validateNonUnitHasNoRemainingUnits(currentItemUnits, deletedItemUnits, createdItemUnits);
    syncNonUnitAvailableQuantity(unavailableQuantity, requestedTotalQuantity);
  }

  private void applyAvailableQuantityDelta(
      List<ItemUnit> deletedItemUnits,
      List<ItemUnit> createdItemUnits
  ) {
    for (ItemUnit deletedItemUnit : deletedItemUnits) {
      if (deletedItemUnit.getStatus() == ItemUnitStatus.AVAILABLE) {
        removeAvailableUnitQuantity();
      }
    }

    for (int i = 0; i < createdItemUnits.size(); i++) {
      addAvailableUnitQuantity();
    }
  }

  private void validateNonUnitToUnitChange(int unavailableQuantity) {
    if (unavailableQuantity > 0) {
      throw new DomainException(
          ErrorCode.CANNOT_CONVERT_NON_UNIT_ITEM_WITH_UNAVAILABLE_QUANTITY_TO_UNIT
      );
    }
  }

  private void validateNonUnitHasNoRemainingUnits(
      List<ItemUnit> currentItemUnits,
      List<ItemUnit> deletedItemUnits,
      List<ItemUnit> createdItemUnits
  ) {
    int finalUnitCount =
        currentItemUnits.size() - deletedItemUnits.size() + createdItemUnits.size();
    if (finalUnitCount != 0) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Non-unit items cannot keep item units after update."
      );
    }
  }

  private void syncNonUnitAvailableQuantity(int unavailableQuantity, Integer requestedTotalQuantity) {
    if (requestedTotalQuantity < unavailableQuantity) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Total quantity cannot be smaller than unavailable quantity."
      );
    }
    this.availableQuantity = requestedTotalQuantity - unavailableQuantity;
  }

  /**
   * 최종 유닛 개수와 요청 totalQuantity 가 다르면
   * 프론트가 보낸 삭제/추가 목록이 현재 수량 정책과 맞지 않는 것이다.
   */
  private void validateUnitQuantityMatches(
      Integer requestedTotalQuantity,
      List<ItemUnit> currentItemUnits,
      List<ItemUnit> deletedItemUnits,
      List<ItemUnit> createdItemUnits
  ) {
    int finalUnitCount =
        currentItemUnits.size() - deletedItemUnits.size() + createdItemUnits.size();
    if (!requestedTotalQuantity.equals(finalUnitCount)) {
      throw new DomainException(ErrorCode.BAD_REQUEST_EXCEPTION, "총 개수와 유닛 삭제/추가 요청이 일치하지 않습니다.");
    }
  }

  /**
   * 
   * 대여 중인 아이템의 갯수를 반환
   * Unit 타입의 경우 Broken, Lost 등의 상태에 있는 itemUnit 에 의해 availableQuantity 가 감소할 수 있으므로 보정하여 반환
   */
  public int getRentedQuantity() {
    if (this.itemManagementType == ItemManagementType.NON_UNIT) {
      return totalQuantity - availableQuantity;
    }

    int unavailableCount = (int) getItemUnits().stream()
        .filter(ItemUnit::isUnavailable)
        .count();

    return totalQuantity - availableQuantity + unavailableCount;
  }
}
