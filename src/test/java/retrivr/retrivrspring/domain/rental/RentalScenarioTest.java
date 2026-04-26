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

public class RentalScenarioTest {

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
  // ============================================================
  // 1) 요청(Request)
  // ============================================================

  @Test
  @DisplayName("요청 정상: UNIT 대여요청 시 unit=RENTAL_PENDING, availableQuantity 1 감소")
  void request_ok_unitPending_and_quantityDecrease() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // when: “요청” 단계에서 도메인 효과 발생
    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    assertThat(rental.getOrganization()).isSameAs(org);
    assertThat(rental.getBorrower()).isSameAs(borrower);
    assertThat(rental.getItem()).isSameAs(item);
    assertThat(rental.getItemUnit()).isSameAs(unit);
    assertThat(rental.getRequestedAt()).isNotNull();

    assertThat(rental.getDecidedAt()).isNull();
    assertThat(rental.getDecidedBy()).isNull();
    assertThat(rental.getDueDate()).isNull();
    assertThat(rental.getReceivedBy()).isNull();
    assertThat(rental.getReturnedAt()).isNull();


    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.RENTAL_PENDING);
    assertThat(item.getAvailableQuantity()).isEqualTo(0);
  }
  @Test
  @DisplayName("요청 예외(UNIT): UNIT 관리 타입인데 itemUnit=null이면 요청 불가")
  void request_fail_unitType_whenItemUnitNull() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    Borrower borrower = borrower();

    // when
    Throwable t = catchThrowable(() -> Rental.request(item, null, borrower, "public-id"));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE);
  }

  @Test
  @DisplayName("요청 예외: UNIT이 AVAILABLE이 아니면 요청 불가")
  void request_fail_whenUnitNotAvailable() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.RENTED); // ItemUnit 이 대여 불가능 상태
    Borrower borrower = borrower();

    // when
    Throwable t = catchThrowable(() -> Rental.request(item, unit, borrower, "public-id"));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
  }

  @Test
  @DisplayName("요청 예외: availableQuantity가 0인데 요청하면(정책상) 불가여야 함")
  void request_fail_whenNoAvailableQuantity() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 1, 0, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // when
    Throwable t = catchThrowable(() -> Rental.request(item, unit, borrower, "public-id"));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_AVAILABLE_ITEM);
  }

  // ============================================================
  // 2) 승인(Approve)
  // ============================================================

  @Test
  @DisplayName("승인 정상: REQUESTED -> RENTED, unit=RENTED, dueDate 세팅")
  void approve_ok_changesRentalAndUnitStatus() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // 요청 효과 먼저 반영(실제 흐름)
    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // when
    rental.approve("admin", org);

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    assertThat(rental.getDecidedAt()).isNotNull();
    assertThat(rental.getDecidedBy()).isEqualTo("admin");
    assertThat(rental.getDueDate()).isNotNull();
    assertThat(rental.getReceivedBy()).isNull();
    assertThat(rental.getReturnedAt()).isNull();

    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.RENTED);
  }

  @Test
  @DisplayName("승인 예외: 다른 조직이 승인하면 ORGANIZATION_MISMATCH_EXCEPTION")
  void approve_fail_organizationMismatch() {
    // given
    Organization owner = org(1L);
    Organization other = org(2L);

    Item item = unitItem(owner, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // when
    Throwable t = catchThrowable(() -> rental.approve("admin", other));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  @Test
  @DisplayName("승인 예외: REQUESTED가 아닌 상태에서 approve 하면 전이 예외")
  void approve_fail_whenNotRequested() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");
    ReflectionTestUtils.setField(rental, "status", RentalStatus.RENTED); // 강제 세팅

    // when
    Throwable t = catchThrowable(() -> rental.approve("admin", org));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION);
  }

  @Test
  @DisplayName("승인 예외: ItemUnit 이 RENTED_PENDING 이 아닌 상태에서 approve 하면 전이 예외")
  void approve_fail_whenNotRentedPending() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");
    ReflectionTestUtils.setField(unit, "status", ItemUnitStatus.AVAILABLE); // 강제 세팅

    // when
    Throwable t1 = catchThrowable(() -> rental.approve("admin", org));

    // then
    assertThat(t1).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);

    // given 2
    ReflectionTestUtils.setField(unit, "status", ItemUnitStatus.RENTED); // 강제 세팅

    // when 2
    Throwable t2 = catchThrowable(() -> rental.approve("admin", org));

    // then 2
    assertThat(t2).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION);
  }


  // ============================================================
  // 3) 거절(Reject)
  // ============================================================

  @Test
  @DisplayName("거절 정상: REQUESTED -> REJECTED, unit=AVAILABLE 복귀, availableQuantity 1 증가")
  void reject_ok_restoreStock_and_changeStatus() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // when: 거절
    rental.reject("admin", org);

    // then: Rental 상태
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REJECTED);
    assertThat(rental.getDecidedAt()).isNotNull();
    assertThat(rental.getDecidedBy()).isEqualTo("admin");
    assertThat(rental.getDueDate()).isNull();
    assertThat(rental.getReceivedBy()).isNull();
    assertThat(rental.getReturnedAt()).isNull();

    // then: 재고/유닛 복원 (Item.onRentalRejected가 plusOne & unit returned 호출)
    assertThat(item.getAvailableQuantity()).isEqualTo(1);
    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
  }

  @Test
  @DisplayName("거절 예외: 다른 조직이 거절하면 ORGANIZATION_MISMATCH_EXCEPTION")
  void reject_fail_organizationMismatch() {
    // given
    Organization org = org(1L);
    Organization other = org(2L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // when
    Throwable t = catchThrowable(() -> rental.reject("admin", other));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  @Test
  @DisplayName("시스템 거절 정상: REQUESTED -> REJECTED, unit AVAILABLE 복귀, availableQuantity 1 증가")
  void reject_ok_restoreStock_and_changeStatus_whenSystemReject() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    Rental rental = Rental.request(item, unit, borrower, "public-id");

    // when: 거절
    rental.rejectBySystem("SYSTEM");

    // then: Rental 상태
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REJECTED);
    assertThat(rental.getDecidedAt()).isNotNull();
    assertThat(rental.getDecidedBy()).isEqualTo("SYSTEM");
    assertThat(rental.getDueDate()).isNull();
    assertThat(rental.getReceivedBy()).isNull();
    assertThat(rental.getReturnedAt()).isNull();

    // then: 재고/유닛 복원 (Item.onRentalRejected가 plusOne & unit returned 호출)
    assertThat(item.getAvailableQuantity()).isEqualTo(1);
    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
  }

  // ============================================================
  // 4) 반납(Mark Returned)
  // ============================================================

  @Test
  @DisplayName("반납 정상: RENTED -> RETURNED, unit=AVAILABLE, availableQuantity 1 증가")
  void markReturned_ok_restoreStock_and_changeStatus() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // 요청 -> 승인까지 만들어서 RENTED 상태 만들기
    Rental rental = Rental.request(item, unit, borrower, "public-id");
    rental.approve("admin", org);

    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.RENTED);
    assertThat(item.getAvailableQuantity()).isEqualTo(0);

    // when
    rental.markReturned("admin", org);

    // then: Rental
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    assertThat(rental.getReturnedAt()).isNotNull();
    assertThat(rental.getReceivedBy()).isEqualTo("admin");

    // then: 재고/유닛 복원
    assertThat(item.getAvailableQuantity()).isEqualTo(1);
    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.AVAILABLE);
  }

  @Test
  @DisplayName("반납 예외: 다른 조직이 반납하면 ORGANIZATION_MISMATCH_EXCEPTION")
  void markReturned_fail_organizationMismatch() {
    // given
    Organization org = org(1L);
    Organization other = org(2L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // 요청 -> 승인까지 만들어서 RENTED 상태 만들기
    Rental rental = Rental.request(item, unit, borrower, "public-id");
    rental.approve("admin", org);

    assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTED);
    assertThat(unit.getStatus()).isEqualTo(ItemUnitStatus.RENTED);
    assertThat(item.getAvailableQuantity()).isEqualTo(0);

    // when
    Throwable t = catchThrowable(() -> rental.markReturned("admin", other));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
  }

  @Test
  @DisplayName("반납 예외: RENTED가 아닌 상태에서 markReturned 하면 전이 예외")
  void markReturned_fail_whenNotRented() {
    // given
    Organization org = org(1L);
    Item item = unitItem(org, 2, 1, 7);
    ItemUnit unit = unit(item, 100L, ItemUnitStatus.AVAILABLE);
    Borrower borrower = borrower();

    // 요청 -> 승인까지 만들어서 RENTED 상태 만들기
    Rental rental = Rental.request(item, unit, borrower, "public-id");
    rental.approve("admin", org);

    ReflectionTestUtils.setField(rental, "status", RentalStatus.RETURNED);

    // when
    Throwable t = catchThrowable(() -> rental.markReturned("admin", org));

    // then
    assertThat(t).isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION);
  }




}
