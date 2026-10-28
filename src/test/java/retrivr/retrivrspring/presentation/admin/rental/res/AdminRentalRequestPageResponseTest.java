package retrivr.retrivrspring.presentation.admin.rental.res;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentalItemUnit;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalRequestPageResponse.RentalRequestSummary;

class AdminRentalRequestPageResponseTest {

  @Test
  @DisplayName("대여 요청 JSON 응답에 대여자 요구정보의 라벨과 입력값을 포함한다")
  void response_includesBorrowerFieldKeysAndValues() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    Rental rental = mock(Rental.class);
    when(rental.getItem()).thenReturn(mock(Item.class));
    when(rental.getBorrower()).thenReturn(Borrower.create(
        "홍길동", new PhoneNumber("010-1234-5678"),
        objectMapper.valueToTree(Map.of("학번", "202012345", "학과", "컴퓨터공학부"))));

    AdminRentalRequestPageResponse response = new AdminRentalRequestPageResponse(
        List.of(RentalRequestSummary.from(rental)), null);
    var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

    assertThat(json.at("/requests/0/borrowerFields/학번").asText()).isEqualTo("202012345");
    assertThat(json.at("/requests/0/borrowerFields/학과").asText()).isEqualTo("컴퓨터공학부");
  }

  @Test
  @DisplayName("from: unit rental includes itemUnitLabel and requestNote")
  void fromUnit_includesItemUnitLabelAndRequestNote() {
    Rental rental = mock(Rental.class);
    Item item = mock(Item.class);
    ItemUnit itemUnit = mock(ItemUnit.class);
    RentalItemUnit rentalItemUnit = mock(RentalItemUnit.class);
    Borrower borrower = Borrower.create("홍길동", new PhoneNumber("010-1234-5678"), null);

    when(rental.getId()).thenReturn(1L);
    when(rental.getItem()).thenReturn(item);
    when(rental.hasItemUnit()).thenReturn(true);
    when(rental.getRentalItemUnits()).thenReturn(List.of(rentalItemUnit));
    when(rental.getBorrower()).thenReturn(borrower);
    when(rental.getRequestNote()).thenReturn("충전기 포함");
    when(rental.getRequestedAt()).thenReturn(LocalDateTime.of(2026, 5, 2, 12, 0));

    when(item.getId()).thenReturn(10L);
    when(item.getName()).thenReturn("노트북");
    when(item.getRentalDuration()).thenReturn(7);
    when(item.getTotalQuantity()).thenReturn(3);
    when(item.getAvailableQuantity()).thenReturn(1);
    when(item.getGuaranteedGoods()).thenReturn("학생증");

    when(rentalItemUnit.getItemUnit()).thenReturn(itemUnit);
    when(itemUnit.getId()).thenReturn(100L);
    when(itemUnit.getLabel()).thenReturn("unit-001");

    RentalRequestSummary summary = RentalRequestSummary.from(rental);

    assertThat(summary.itemUnitId()).isEqualTo(100L);
    assertThat(summary.itemUnitLabel()).isEqualTo("unit-001");
    assertThat(summary.requestNote()).isEqualTo("충전기 포함");
  }

  @Test
  @DisplayName("from: non-unit rental keeps itemUnitLabel null and includes requestNote")
  void fromNonUnit_includesRequestNote() {
    Rental rental = mock(Rental.class);
    Item item = mock(Item.class);
    Borrower borrower = Borrower.create("홍길동", new PhoneNumber("010-1234-5678"), null);

    when(rental.getId()).thenReturn(2L);
    when(rental.getItem()).thenReturn(item);
    when(rental.hasItemUnit()).thenReturn(false);
    when(rental.getBorrower()).thenReturn(borrower);
    when(rental.getRequestNote()).thenReturn("문 앞 수령");
    when(rental.getRequestedAt()).thenReturn(LocalDateTime.of(2026, 5, 2, 12, 0));

    when(item.getId()).thenReturn(20L);
    when(item.getName()).thenReturn("카메라");
    when(item.getRentalDuration()).thenReturn(3);
    when(item.getTotalQuantity()).thenReturn(5);
    when(item.getAvailableQuantity()).thenReturn(2);
    when(item.getGuaranteedGoods()).thenReturn("신분증");

    RentalRequestSummary summary = RentalRequestSummary.from(rental);

    assertThat(summary.itemUnitId()).isNull();
    assertThat(summary.itemUnitLabel()).isNull();
    assertThat(summary.requestNote()).isEqualTo("문 앞 수령");
    assertThat(summary.borrowerFields()).isEmpty();
  }
}
