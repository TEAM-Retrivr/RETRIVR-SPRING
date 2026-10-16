package retrivr.retrivrspring.controller.admin.item;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;

class AdminItemLabelValidationTest {
  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();

  @AfterAll
  static void closeValidatorFactory() {
    FACTORY.close();
  }

  @Test
  void createLabelAllows255AndRejects256Characters() {
    assertThat(VALIDATOR.validate(createRequest("가".repeat(255)))).isEmpty();
    assertThat(VALIDATOR.validate(createRequest("가".repeat(256))))
        .singleElement().satisfies(violation ->
            assertThat(violation.getPropertyPath().toString()).startsWith("unitLabels"));
  }

  @Test
  void updateValidatesNestedLabelsForBothCreationAndRename() {
    for (AdminItemUnitChangeRequest change : List.of(
        new AdminItemUnitChangeRequest(null, "가".repeat(256)),
        new AdminItemUnitChangeRequest(1L, "가".repeat(256)))) {
      assertThat(VALIDATOR.validate(updateRequest(change)))
          .singleElement().satisfies(violation ->
              assertThat(violation.getPropertyPath().toString()).isEqualTo("unitChanges[0].label"));
    }
    assertThat(VALIDATOR.validate(updateRequest(
        new AdminItemUnitChangeRequest(1L, "가".repeat(255))))).isEmpty();
  }

  @Test
  void nullLabelStillAllowsDeletion() {
    assertThat(VALIDATOR.validate(updateRequest(
        new AdminItemUnitChangeRequest(1L, null)))).isEmpty();
  }

  private AdminItemCreateRequest createRequest(String label) {
    return new AdminItemCreateRequest("item", null, 3, 1, ItemManagementType.UNIT,
        false, null, List.of(label), List.of());
  }

  private AdminItemUpdateRequest updateRequest(AdminItemUnitChangeRequest change) {
    return new AdminItemUpdateRequest("item", null, 3, 1, ItemManagementType.UNIT,
        false, null, List.of(change), List.of(), true, "token");
  }
}
