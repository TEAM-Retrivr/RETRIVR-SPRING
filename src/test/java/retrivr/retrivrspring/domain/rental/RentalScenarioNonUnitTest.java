package retrivr.retrivrspring.domain.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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

public class RentalScenarioNonUnitTest {

  private Organization org(long id) {
    return Organization.builder()
        .id(id)
        .email("org" + id + "@test.com")
        .passwordHash("x")
        .name("org" + id)
        .adminCodeHash("x")
        .build();
  }

  private Item nonUnitItem(Organization org, int totalQty, int availableQty, int rentalDurationDays) {
    return Item.builder()
        .id(20L)
        .organization(org)
        .name("도서(비유닛)")
        .rentalDuration(rentalDurationDays)
        .useMessageAlarmService(false)
        .totalQuantity(totalQty)
        .availableQuantity(availableQty)
        .isActive(true)
        .itemManagementType(ItemManagementType.NON_UNIT)
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

  // ============================================================
  // 1) 요청(Request)
  // ============================================================

  @Test
  @DisplayName("요청 정상(NON_UNIT): 대여요청 시 availableQuantity 1 감소, itemUnit=null")
  void request_ok_nonUnit_quantityDecrease() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    Borrower borrower = borrower();

    // when
    Rental rental = Rental.request(item, null, borrower, "public-id");

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    assertThat(rental.getOrganization()).isSameAs(org);
    assertThat(rental.getBorrower()).isSameAs(borrower);
    assertThat(rental.getItem()).isSameAs(item);
    assertThat(rental.getItemUnit()).isNull();
    assertThat(rental.getRequestedAt()).isNotNull();

    assertThat(item.getAvailableQuantity()).isEqualTo(1);
  }

  @Test
  @DisplayName("요청 예외(NON_UNIT): availableQuantity가 0이면 요청 불가")
  void request_fail_nonUnit_whenNoAvailableQuantity() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 1, 0, 7);
    Borrower borrower = borrower();

    // when
    Throwable t = catchThrowable(() -> Rental.request(item, null, borrower, "public-id"));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_AVAILABLE_ITEM);
  }

  @Test
  @DisplayName("요청 예외(NON_UNIT): NON_UNIT 관리 타입인데 itemUnit이 들어오면 요청 불가")
  void request_fail_nonUnitType_whenItemUnitProvided() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // when
    Throwable t = catchThrowable(() -> Rental.request(item, unit, borrower, "public-id"));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
  }
  // ============================================================
  // 2) 승인(Approve)
  // ============================================================

  @Test
  @DisplayName("승인 정상(NON_UNIT): REQUESTED -> RENTED, dueDate 세팅 (수량은 요청에서 이미 빠짐)")
  void approve_ok_nonUnit() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, null, borrower, "public-id");

    // when
    rental.approve("admin", org);

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    assertThat(rental.getDecidedAt()).isNotNull();
    assertThat(rental.getDecidedBy()).isEqualTo("admin");
    assertThat(rental.getDueDate()).isNotNull();

    assertThat(item.getAvailableQuantity()).isEqualTo(1);
  }

  @Test
  @DisplayName("승인 예외(NON_UNIT): 다른 조직이 승인하면 ORGANIZATION_MISMATCH_EXCEPTION")
  void approve_fail_nonUnit_organizationMismatch() {
    // given
    Organization owner = org(1L);
    Organization other = org(2L);

    Item item = nonUnitItem(owner, 3, 2, 7);
    Borrower borrower = borrower();
    Rental rental = Rental.request(item, null, borrower, "public-id");

    // when
    Throwable t = catchThrowable(() -> rental.approve("admin", other));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  // ============================================================
  // 3) 거절(Reject)
  // ============================================================

  @Test
  @DisplayName("거절 정상(NON_UNIT): REQUESTED -> REJECTED, availableQuantity 1 증가(복원)")
  void reject_ok_nonUnit_restoreQuantity() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, null, borrower, "public-id");
    assertThat(item.getAvailableQuantity()).isEqualTo(1); // request에서 -1

    // when
    rental.reject("admin", org);

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REJECTED);
    assertThat(rental.getDecidedAt()).isNotNull();
    assertThat(rental.getDecidedBy()).isEqualTo("admin");

    assertThat(item.getAvailableQuantity()).isEqualTo(2); // reject에서 +1 복원
  }

  // ============================================================
  // 4) 반납(Mark Returned)
  // ============================================================

  @Test
  @DisplayName("반납 정상(NON_UNIT): RENTED -> RETURNED, availableQuantity 1 증가")
  void markReturned_ok_nonUnit_restoreQuantity() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, null, borrower, "public-id");
    rental.approve("admin", org);
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    assertThat(item.getAvailableQuantity()).isEqualTo(1);

    // when
    rental.markReturned("admin", org);

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    assertThat(rental.getReturnedAt()).isNotNull();
    assertThat(rental.getReceivedBy()).isEqualTo("admin");

    assertThat(item.getAvailableQuantity()).isEqualTo(2); // +1
  }

  @Test
  @DisplayName("반납 예외(NON_UNIT): RENTED가 아니면 전이 예외")
  void markReturned_fail_nonUnit_whenNotRented() {
    // given
    Organization org = org(1L);
    Item item = nonUnitItem(org, 3, 2, 7);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, null, borrower, "public-id");
    ReflectionTestUtils.setField(rental, "status", RentalStatus.RETURNED);

    // when
    Throwable t = catchThrowable(() -> rental.markReturned("admin", org));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION);
  }
}
