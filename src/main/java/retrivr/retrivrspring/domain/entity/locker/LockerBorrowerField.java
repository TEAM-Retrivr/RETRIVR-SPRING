package retrivr.retrivrspring.domain.entity.locker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Table(name = "locker_borrower_field")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerBorrowerField {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "policy_id", nullable = false)
  private LockerPolicy policy;

  @Column(nullable = false, length = 80)
  private String fieldKey;

  @Column(nullable = false, length = 100)
  private String label;

  @Column(nullable = false)
  private boolean required;

  @Column(nullable = false)
  private int sortOrder;

  public static LockerBorrowerField create(LockerPolicy policy, String fieldKey, String label, boolean required, int sortOrder) {
    LockerBorrowerField instance = LockerBorrowerField.builder()
        .policy(policy)
        .fieldKey(fieldKey)
        .label(label)
        .required(required)
        .sortOrder(sortOrder)
        .build();
    instance.validateForCreation();
    return instance;
  }

  /**
   * Column Validate Method
   */
  private void validateForCreation() {
    validatePolicy(this.policy);
    validateFieldKey(this.fieldKey);
    validateLabel(this.label);
  }

  private void validatePolicy(LockerPolicy policy) {
    if (policy == null) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_BORROWER_FIELD_VALUE, "사물함 정책은 필수입니다.");
    }
  }

  private void validateFieldKey(String fieldKey) {
    if (fieldKey == null || fieldKey.isBlank() || fieldKey.codePointCount(0, fieldKey.length()) > 80) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_BORROWER_FIELD_VALUE, "필드 키는 공백이 아닌 80자 이하여야 합니다.");
    }
  }

  private void validateLabel(String label) {
    if (label == null || label.isBlank() || label.codePointCount(0, label.length()) > 100) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_BORROWER_FIELD_VALUE, "필드 라벨은 공백이 아닌 100자 이하여야 합니다.");
    }
  }

}
