package retrivr.retrivrspring.application.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("itemUnitId로 이름 변경 대상을 찾는다")
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
  @DisplayName("label이 같아도 itemUnitId로 삭제 대상을 구분한다")
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
  @DisplayName("label을 유닛 중복 식별자로 사용하지 않는다")
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
  @DisplayName("변경 요청에 동일한 itemUnitId가 중복되면 거부한다")
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
  @DisplayName("공백 label을 삭제 요청으로 처리하지 않고 거부한다")
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
  @DisplayName("현재 물품에 존재하지 않는 itemUnitId를 거부한다")
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
