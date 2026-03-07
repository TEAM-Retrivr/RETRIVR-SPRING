package retrivr.retrivrspring.domain.item;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.rental.enumerate.BorrowerFieldType;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

class ItemBorrowerFieldValidationTest extends ItemTestFixture {

  @Nested
  @DisplayName("validationItemBorrowerFieldsWith")
  class ValidationItemBorrowerFieldsWithTest {

    @Test
    @DisplayName("values가 null이면 예외가 발생한다")
    void throwsWhenValuesIsNull() {
      Item item = createItemWithBorrowerFields(
          borrowerField("studentNo", BorrowerFieldType.TEXT, true),
          borrowerField("email", BorrowerFieldType.EMAIL, true)
      );

      assertThatThrownBy(() -> item.validationItemBorrowerFieldsWith(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ILLEGAL_BORROWER_FIELD);
    }

    @Test
    @DisplayName("정의되지 않은 fieldKey가 들어오면 예외가 발생한다")
    void throwsWhenUnknownFieldKeyExists() {
      Item item = createItemWithBorrowerFields(
          borrowerField("studentNo", BorrowerFieldType.TEXT, true)
      );

      Map<String, String> values = Map.of(
          "studentNo", "20201234",
          "unknown", "value"
      );

      assertThatThrownBy(() -> item.validationItemBorrowerFieldsWith(values))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ILLEGAL_BORROWER_FIELD);
    }

    @Test
    @DisplayName("필수 필드가 비어 있으면 예외가 발생한다")
    void throwsWhenRequiredFieldMissing() {
      Item item = createItemWithBorrowerFields(
          borrowerField("studentNo", BorrowerFieldType.TEXT, true),
          borrowerField("email", BorrowerFieldType.EMAIL, true)
      );

      Map<String, String> values = Map.of(
          "studentNo", "20201234",
          "email", ""
      );

      assertThatThrownBy(() -> item.validationItemBorrowerFieldsWith(values))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ILLEGAL_BORROWER_FIELD);
    }

    @Test
    @DisplayName("EMAIL 필드 형식이 잘못되면 예외가 발생한다")
    void throwsWhenEmailFormatIsInvalid() {
      Item item = createItemWithBorrowerFields(
          borrowerField("email", BorrowerFieldType.EMAIL, true)
      );

      Map<String, String> values = Map.of("email", "invalid-email");

      assertThatThrownBy(() -> item.validationItemBorrowerFieldsWith(values))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ILLEGAL_BORROWER_FIELD);
    }

    @Test
    @DisplayName("PHONE 필드 형식이 잘못되면 예외가 발생한다")
    void throwsWhenPhoneFormatIsInvalid() {
      Item item = createItemWithBorrowerFields(
          borrowerField("phone", BorrowerFieldType.PHONE, true)
      );

      Map<String, String> values = Map.of("phone", "abcde");

      assertThatThrownBy(() -> item.validationItemBorrowerFieldsWith(values))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ILLEGAL_BORROWER_FIELD);
    }

    @Test
    @DisplayName("optional 필드는 빈 값이면 타입 검증을 생략하고 통과한다")
    void passesWhenOptionalFieldIsBlank() {
      Item item = createItemWithBorrowerFields(
          borrowerField("studentNo", BorrowerFieldType.TEXT, true),
          borrowerField("phone", BorrowerFieldType.PHONE, false)
      );

      Map<String, String> values = Map.of(
          "studentNo", "20201234",
          "phone", ""
      );

      assertThatCode(() -> item.validationItemBorrowerFieldsWith(values))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 borrower field 값이 유효하면 통과한다")
    void passesWhenAllBorrowerFieldsAreValid() {
      Item item = createItemWithBorrowerFields(
          borrowerField("studentNo", BorrowerFieldType.TEXT, true),
          borrowerField("email", BorrowerFieldType.EMAIL, true),
          borrowerField("phone", BorrowerFieldType.PHONE, false),
          borrowerField("memo", BorrowerFieldType.TEXTAREA, false)
      );

      Map<String, String> values = Map.of(
          "studentNo", "20201234",
          "email", "test@example.com",
          "phone", "010-1234-5678",
          "memo", "비고입니다."
      );

      assertThatCode(() -> item.validationItemBorrowerFieldsWith(values))
          .doesNotThrowAnyException();
    }
  }
}