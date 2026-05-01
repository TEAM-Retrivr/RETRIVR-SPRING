package retrivr.retrivrspring.presentation.admin.rental.res;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminReturnItemUnitListPageResponse.BorrowedItemSummary;

class AdminReturnItemUnitListPageResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("fromUnit: itemUnitLabel and requestNote are included")
  void fromUnit_includesItemUnitLabelAndRequestNote() {
    ItemUnit itemUnit = mock(ItemUnit.class);
    when(itemUnit.getId()).thenReturn(10L);
    when(itemUnit.getLabel()).thenReturn("unit-001");

    BorrowedItemSummary summary = BorrowedItemSummary.fromUnit(itemUnit, rental("문 앞 수령"));

    assertThat(summary.unitId()).isEqualTo(10L);
    assertThat(summary.borrowedItemName()).isEqualTo("unit-001");
    assertThat(summary.itemUnitLabel()).isEqualTo("unit-001");
    assertThat(summary.requestNote()).isEqualTo("문 앞 수령");
  }

  @Test
  @DisplayName("fromNonUnit: itemUnitLabel is null and requestNote is included")
  void fromNonUnit_includesRequestNote() {
    Item item = mock(Item.class);
    when(item.getName()).thenReturn("카메라");

    BorrowedItemSummary summary = BorrowedItemSummary.fromNonUnit(item, rental("삼각대 포함"));

    assertThat(summary.unitId()).isNull();
    assertThat(summary.borrowedItemName()).isEqualTo("카메라");
    assertThat(summary.itemUnitLabel()).isNull();
    assertThat(summary.requestNote()).isEqualTo("삼각대 포함");
  }

  private Rental rental(String requestNote) {
    Rental rental = mock(Rental.class);
    Borrower borrower = Borrower.create(
        "홍길동",
        new PhoneNumber("010-1234-5678"),
        objectMapper.valueToTree(Map.of("학과", "컴공"))
    );

    when(rental.getId()).thenReturn(1L);
    when(rental.isOverdue()).thenReturn(false);
    when(rental.getBorrower()).thenReturn(borrower);
    when(rental.getRequestNote()).thenReturn(requestNote);
    when(rental.getDecidedAt()).thenReturn(LocalDateTime.of(2026, 5, 1, 12, 0));
    when(rental.getDueDate()).thenReturn(LocalDate.of(2026, 5, 8));
    return rental;
  }
}
