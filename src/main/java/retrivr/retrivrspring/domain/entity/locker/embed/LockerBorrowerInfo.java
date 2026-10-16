package retrivr.retrivrspring.domain.entity.locker.embed;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LockerBorrowerInfo {

  @Column(name = "borrower_name", nullable = false)
  private String name;

  @Column(name = "borrower_phone", nullable = false)
  private String phone;

  @Column(name = "borrower_email")
  private String email;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "borrower_additional_info", columnDefinition = "jsonb")
  private JsonNode additionalInfo;

  public static LockerBorrowerInfo create(String name, String phone, String email, JsonNode additionalInfo) {
    LockerBorrowerInfo instance = LockerBorrowerInfo.builder()
        .name(name)
        .phone(phone)
        .email(email)
        .additionalInfo(additionalInfo)
        .build();
    instance.validate();
    return instance;
  }

  public void validate() {
    validateName(this.name);
    validatePhone(this.phone);
    validateEmail(this.email);
  }

  private void validateName(String name) {
    if (name == null || name.isBlank() || name.codePointCount(0, name.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여자 이름은 공백이 아닌 255자 이하여야 합니다.");
    }
  }

  private void validatePhone(String phone) {
    if (phone == null || phone.isBlank() || phone.codePointCount(0, phone.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여자 전화번호는 공백이 아닌 255자 이하여야 합니다.");
    }
  }

  private void validateEmail(String email) {
    if (email != null && email.codePointCount(0, email.length()) > 255) {
      throw new DomainException(ErrorCode.INVALID_LOCKER_RENTAL_VALUE, "대여자 이메일은 255자 이하여야 합니다.");
    }
  }
}
