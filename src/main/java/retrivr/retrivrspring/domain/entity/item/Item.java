package retrivr.retrivrspring.domain.entity.item;

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
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
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

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "item")
  List<ItemBorrowerField> itemBorrowerFields = new ArrayList<>();

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

  public boolean isRentalAble() {
    return availableQuantity > 0;
  }

  public void minusOneAvailableQuantity() {
    if (this.availableQuantity == 0) {
      throw new DomainException(ErrorCode.QUANTITY_CAN_NOT_BE_NEGATIVE);
    }
    availableQuantity--;
  }
}