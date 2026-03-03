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

  @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
  private List<ItemUnit> itemUnits;

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "item")
  List<ItemBorrowerField> itemBorrowerFields = new ArrayList<>();

  public boolean isRentalAble() {
    if (!this.isActive) {
      return false;
    }
    return availableQuantity > 0;
  }

  public boolean isUnitType() {
    if (this.itemManagementType == null) {
      throw new DomainException(ErrorCode.INVALID_ITEM, "물건에는 물건 유형이 지정되어 있어야 합니다.");
    }
    return this.itemManagementType.equals(ItemManagementType.UNIT);
  }

  public void onRentalRequested(@Nullable ItemUnit itemUnit) {
    // 1. 대여 가능 여부 확인
    if (!isRentalAble()) {
      throw new DomainException(ErrorCode.NOT_AVAILABLE_ITEM);
    }
    // 2. itemUnit 이 null 일 경우 Non Unit Type 전용 메소드 호축
    if (itemUnit == null) {
      onRentalRequestedForNonUnitType();
      return;
    }

    // 3. Unit 타입이 아닐 경우 예외 처리
    if (!this.isUnitType()) {
      throw new DomainException(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
    }
    // 4. ItemUnit 이 해당 Item 소유가 아닐 경우 예외 처리
    if (!itemUnit.isBelongTo(this)) {
      throw new DomainException(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
    }

    // 5. ItemUnit 의 대여 요청 처리
    itemUnit.onRentalRequested();
    // 6. 해당 Item 의 수량 감소
    minusOneAvailableQuantity();
  }
  
  private void onRentalRequestedForNonUnitType() {
    // 1. Unit 타입일 경우 예외
    if (this.isUnitType()) {
      throw new DomainException(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
    }
    // 2. 해당 Item 의 수량 감소
    minusOneAvailableQuantity();
  }

  public void onRentalApprove(@Nullable ItemUnit itemUnit) {
    if (itemUnit == null) {
      return;
    }
    if (!itemUnit.isBelongTo(this)) {
      throw new DomainException(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
    }
    itemUnit.onRentalApprove();
  }

  public void onRentalRejected(@Nullable ItemUnit itemUnit) {
    if (itemUnit == null) {
      plusOneAvailableQuantity();
      return;
    }
    if (!itemUnit.isBelongTo(this)) {
      throw new DomainException(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
    }
    itemUnit.onRentalRejected();
  }

  public void onRentalReturned() {
    plusOneAvailableQuantity();
  }

  private void plusOneAvailableQuantity() {
    if (this.availableQuantity >= this.totalQuantity) {
      throw new DomainException(ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION);
    }
    availableQuantity++;
  }

  private void minusOneAvailableQuantity() {
    if (this.availableQuantity <= 0) {
      throw new DomainException(ErrorCode.AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION);
    }
    availableQuantity--;
  }

  public void validationItemBorrowerFieldsWith(Map<String, String> values) {
    if (values == null) {
      throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Borrower fields map is null");
    }

    // 1) unknown key 검증: 정의되지 않은 fieldKey가 들어오면 에러
    Set<String> allowedKeys = new HashSet<>();
    for (ItemBorrowerField field : itemBorrowerFields) {
      allowedKeys.add(field.getFieldKey());
    }

    for (String key : values.keySet()) {
      if (!allowedKeys.contains(key)) {
        throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Unknown borrower field key: " + key);
      }
    }

    // 2) required + type 검증
    for (ItemBorrowerField field : itemBorrowerFields) {
      String key = field.getFieldKey();
      String raw = values.get(key);

      // required 체크
      if (field.isRequired()) {
        if (raw == null || raw.isBlank()) {
          throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Required borrower field missing: " + key);
        }
      }

      // 값이 없으면 타입 검증 스킵(옵션 필드)
      if (raw == null || raw.isBlank()) {
        continue;
      }

      // type 체크
      field.validateType(raw, key);
    }
  }
}