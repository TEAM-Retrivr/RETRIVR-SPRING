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
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalSearchPageResponse.RentalSearchSummary;

class AdminRentalSearchPageResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("unit rental search result includes overdue and return-management fields")
  void from_unitRentalIncludesReturnManagementFields() {
    Borrower borrower = Borrower.create(
        "이리트",
        new PhoneNumber("010-1234-5678"),
        objectMapper.valueToTree(Map.of(
            "additionalProp1", "컴퓨터공학과",
            "additionalProp2", "20260430",
            "additionalProp3", ""
        ))
    );
    Rental rental = rental(borrower, true);
    ItemUnit itemUnit = mock(ItemUnit.class);
    when(itemUnit.getLabel()).thenReturn("c타입 충전기(1)");
    when(rental.getItemUnit()).thenReturn(itemUnit);
    when(rental.getRequestNote()).thenReturn("오후 반납");
    when(rental.getDecidedBy()).thenReturn("관리자");

    RentalSearchSummary result = RentalSearchSummary.from(rental);

    assertThat(result.rentalId()).isEqualTo(1L);
    assertThat(result.borrowerName()).isEqualTo("이리트");
    assertThat(result.contact()).isEqualTo("010-1234-5678");
    assertThat(result.itemName()).isEqualTo("c타입 충전기");
    assertThat(result.isOverdue()).isTrue();
    assertThat(result.itemUnitLabel()).isEqualTo("c타입 충전기(1)");
    assertThat(result.rentalDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(result.expectedReturnDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(result.borrowerFields()).containsExactly(
        Map.entry("additionalProp1", "컴퓨터공학과"),
        Map.entry("additionalProp2", "20260430"),
        Map.entry("additionalProp3", "")
    );
    assertThat(result.requestNote()).isEqualTo("오후 반납");
    assertThat(result.approvalAdminName()).isEqualTo("관리자");
  }

  @Test
  @DisplayName("non-unit non-overdue search result keeps optional fields null or empty")
  void from_nonUnitRentalKeepsOptionalFieldsNullOrEmpty() {
    Borrower borrower = Borrower.create("이리트", new PhoneNumber("010-1234-5678"), null);
    Rental rental = rental(borrower, false);

    RentalSearchSummary result = RentalSearchSummary.from(rental);

    assertThat(result.isOverdue()).isFalse();
    assertThat(result.itemUnitLabel()).isNull();
    assertThat(result.borrowerFields()).isEmpty();
    assertThat(result.requestNote()).isNull();
    assertThat(result.approvalAdminName()).isNull();
  }

  private Rental rental(Borrower borrower, boolean overdue) {
    Rental rental = mock(Rental.class);
    Item item = mock(Item.class);
    when(item.getName()).thenReturn("c타입 충전기");
    when(rental.getId()).thenReturn(1L);
    when(rental.getBorrower()).thenReturn(borrower);
    when(rental.getItem()).thenReturn(item);
    when(rental.isOverdue()).thenReturn(overdue);
    when(rental.getDecidedAt()).thenReturn(LocalDateTime.of(2026, 9, 1, 13, 30));
    when(rental.getDueDate()).thenReturn(LocalDate.of(2026, 9, 10));
    return rental;
  }
}
