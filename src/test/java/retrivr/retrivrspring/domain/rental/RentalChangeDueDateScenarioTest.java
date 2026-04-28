package retrivr.retrivrspring.domain.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

public class RentalChangeDueDateScenarioTest {

  private Organization org(long id) {
    return Organization.builder()
        .id(id)
        .email("org"+ id + "@test.com")
        .passwordHash("x")
        .name("org" + id)
        .adminCodeHash("x")
        .build();
  }

  private Item unitItem(Organization org, int totalQty, int availableQty, int rentalDurationDays) {
    return Item.builder()
        .id(10L)
        .organization(org)
        .name("카메라")
        .rentalDuration(rentalDurationDays)
        .useMessageAlarmService(false)
        .totalQuantity(totalQty)
        .availableQuantity(availableQty)
        .isActive(true)
        .itemManagementType(ItemManagementType.UNIT)
        .build();
  }

  private ItemUnit unit(Item item, long id, ItemUnitStatus status) {
    return ItemUnit.builder()
        .id(id)
        .item(item)
        .label("UNIT-" + id)
        .status(status)
        .build();
  }

  private Borrower borrower() {
    return Borrower.builder()
        .name("홍길동")
        .phone(new PhoneNumber("010-0000-0000"))
        .additionalBorrowerInfo(null)
        .build();
  }

  private Rental rentedRental(Organization org) {
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");
    rental.approve("admin", org);
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    return rental;
  }

  @Test
  @DisplayName("대여기간 수정 정상: RENTED 상태에서 dueDate 변경 가능")
  void changeDueDate_ok_whenRented() {
    // given
    Organization org = org(1L);
    Rental rental = rentedRental(org);

    LocalDate oldDue = rental.getDueDate();
    LocalDate newDue = oldDue.plusDays(3);

    // when
    rental.changeDueDate(newDue, org);

    // then
    assertThat(rental.getDueDate()).isEqualTo(newDue);
  }

  @Test
  @DisplayName("대여기간 수정 예외: newDueDate=null이면 RENTAL_DUE_DATE_UPDATE_EXCEPTION")
  void changeDueDate_fail_whenNull() {
    // given
    Organization org = org(1L);
    Rental rental = rentedRental(org);

    // when
    Throwable t = catchThrowable(() -> rental.changeDueDate(null, org));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION);
  }

  @Test
  @DisplayName("대여기간 수정 예외: 다른 조직이면 ORGANIZATION_MISMATCH_EXCEPTION")
  void changeDueDate_fail_organizationMismatch() {
    // given
    Organization owner = org(1L);
    Organization other = org(2L);
    Rental rental = rentedRental(owner);

    // when
    Throwable t = catchThrowable(() -> rental.changeDueDate(rental.getDueDate().plusDays(1), other));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  @Test
  @DisplayName("대여기간 수정 예외: RENTED가 아닌 상태에서 changeDueDate 호출 시 전이 예외")
  void changeDueDate_fail_whenNotRented() {
    // given
    Organization org = org(1L);
    Rental rental = rentedRental(org);

    ReflectionTestUtils.setField(rental, "status", RentalStatus.REQUESTED);

    // when
    Throwable t = catchThrowable(() -> rental.changeDueDate(LocalDate.now().plusDays(1), org));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION);
  }
}
