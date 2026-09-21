package retrivr.retrivrspring.application.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.application.service.admin.item.support.AdminItemUnitChangeClassifier;
import retrivr.retrivrspring.application.service.admin.item.support.AdminItemUnitChangeClassifier.AdminItemUnitChangeSet;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;

class AdminItemUnitChangeClassifierTest {

  private final AdminItemUnitChangeClassifier classifier = new AdminItemUnitChangeClassifier();

  @Test
  void classify_resolvesRenameTargetByItemUnitId() {
    ItemUnit firstUnit = itemUnit(101L, "unit-a");
    ItemUnit secondUnit = itemUnit(102L, "unit-b");

    AdminItemUnitChangeSet result = classifier.classify(
        List.of(firstUnit, secondUnit),
        List.of(new AdminItemUnitChangeRequest(102L, "renamed-unit"))
    );

    assertThat(result.renameCommands()).hasSize(1);
    assertThat(result.renameCommands().getFirst().itemUnit()).isSameAs(secondUnit);
    assertThat(result.renameCommands().getFirst().label()).isEqualTo("renamed-unit");
  }

  @Test
  void classify_resolvesDeleteTargetByItemUnitIdWhenLabelsAreSame() {
    ItemUnit firstUnit = itemUnit(101L, "same-label");
    ItemUnit secondUnit = itemUnit(102L, "same-label");

    AdminItemUnitChangeSet result = classifier.classify(
        List.of(firstUnit, secondUnit),
        List.of(new AdminItemUnitChangeRequest(101L, null))
    );

    assertThat(result.deleteItemUnits()).containsExactly(firstUnit);
  }

  @Test
  void classify_doesNotUseLabelAsDuplicateIdentifier() {
    ItemUnit firstUnit = itemUnit(101L, "unit-a");
    ItemUnit secondUnit = itemUnit(102L, "same-label");

    AdminItemUnitChangeSet result = classifier.classify(
        List.of(firstUnit, secondUnit),
        List.of(
            new AdminItemUnitChangeRequest(101L, "same-label"),
            new AdminItemUnitChangeRequest(null, "same-label")
        )
    );

    assertThat(result.renameCommands()).hasSize(1);
    assertThat(result.renameCommands().getFirst().itemUnit()).isSameAs(firstUnit);
    assertThat(result.createLabels()).containsExactly("same-label");
  }

  @Test
  void classify_rejectsDuplicatedItemUnitId() {
    ItemUnit itemUnit = itemUnit(101L, "unit-a");

    assertThatThrownBy(() -> classifier.classify(
        List.of(itemUnit),
        List.of(
            new AdminItemUnitChangeRequest(101L, null),
            new AdminItemUnitChangeRequest(101L, "renamed-unit")
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DUPLICATE_ITEM_UNIT_ID_IN_REQUEST);
  }

  @Test
  void classify_rejectsBlankLabelInsteadOfTreatingItAsDelete() {
    ItemUnit itemUnit = itemUnit(101L, "unit-a");

    assertThatThrownBy(() -> classifier.classify(
        List.of(itemUnit),
        List.of(new AdminItemUnitChangeRequest(101L, "   "))
    ))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.BAD_REQUEST_EXCEPTION);
  }

  @Test
  void classify_rejectsUnknownItemUnitId() {
    ItemUnit itemUnit = itemUnit(101L, "unit-a");

    assertThatThrownBy(() -> classifier.classify(
        List.of(itemUnit),
        List.of(new AdminItemUnitChangeRequest(999L, "renamed-unit"))
    ))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ITEM_UNIT);
  }

  private ItemUnit itemUnit(Long id, String label) {
    return ItemUnit.builder()
        .id(id)
        .label(label)
        .build();
  }
}
