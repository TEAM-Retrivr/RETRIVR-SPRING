package retrivr.retrivrspring.domain.entity.rental;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@Embeddable
@NoArgsConstructor
public class PhoneNumber {

  private String phone;

  public PhoneNumber(String phone) {
    this.phone = normalize(phone);

    if (!isValid()) {
      throw new DomainException(ErrorCode.INVALID_PHONE_NUMBER_EXCEPTION);
    }
  }

  private String normalize(String phone) {
    if (phone == null) {
      return null;
    }
    return phone.replaceAll("\\D", "");
  }

  public boolean isValid() {
    return this.phone != null && this.phone.matches("^010\\d{8}$");
  }
}
