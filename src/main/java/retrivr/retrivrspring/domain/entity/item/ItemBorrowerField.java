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
import jakarta.persistence.Table;
import java.time.format.DateTimeParseException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.rental.enumerate.BorrowerFieldType;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item_borrower_field")
public class ItemBorrowerField extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_borrower_field_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Column(nullable = false, length = 255)
  private String fieldKey;

  @Column(nullable = false, length = 255)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 30)
  private BorrowerFieldType fieldType;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public void validateType(String raw, String key) {
    try {
      switch (fieldType) {

        case TEXT -> {
          // 아무 검증 안 해도 됨(길이 제한은 DB/DTO validation으로)
        }

        case TEXTAREA -> {
          // TEXT와 동일하게 별도 검증 불필요
        }

        case EMAIL -> {
          // 간단 검증 (정교한건 @Email 추천)
          String v = raw.trim();
          if (!v.contains("@") || v.startsWith("@") || v.endsWith("@")) {
            throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Field must be email: " + key);
          }
        }

        case PHONE -> {
          // 숫자/하이픈만 허용 같은 룰로 간단 검증
          String v = raw.trim();
          if (!v.matches("[0-9\\-+ ]{7,20}")) {
            throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Field must be phone: " + key);
          }
        }

        default -> {
          // enum이 확장되었지만 추가하지 않았을 경우 방어
          throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Unsupported borrower field type: " + fieldType + ", key=" + key);
        }
      }
    } catch (NumberFormatException | DateTimeParseException e) {
      throw new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "Invalid value for field type " + fieldType + ": key=" + key + ", value=" + raw);
    }
  }
}
