package retrivr.retrivrspring.domain.entity.rental;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "borrower")
public class Borrower extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "borrower_id")
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Embedded
  private PhoneNumber phone;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "additional_borrower_info", columnDefinition = "jsonb")
  private JsonNode additionalBorrowerInfo;

  public static Borrower create(String name, PhoneNumber phone, JsonNode additionalBorrowerInfo) {
    return Borrower.builder()
        .name(name)
        .phone(phone)
        .additionalBorrowerInfo(additionalBorrowerInfo)
        .build();
  }

  public boolean hasAdditionalInfo() {
    return additionalBorrowerInfo != null
        && !additionalBorrowerInfo.isEmpty()
        && !additionalBorrowerInfo.isNull();
  }

  public boolean isValidPhoneFormat() {
    return phone.isValid();
  }

  public String getEmail() {
    if (additionalBorrowerInfo == null || additionalBorrowerInfo.isNull()) {
      return null;
    }

    JsonNode emailNode = additionalBorrowerInfo.get("email");
    if (emailNode == null || emailNode.isNull()) {
      return null;
    }

    String email = emailNode.asText(null);
    if (email == null) {
      return null;
    }

    String trimmed = email.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public String getPhoneNumber() {
    return phone == null ? null : phone.getPhone();
  }

  public String getAllAdditionalInfo() {
    return additionalBorrowerInfo.toString();
  }
}
