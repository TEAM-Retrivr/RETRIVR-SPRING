package retrivr.retrivrspring.presentation.locker.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerExtensionStatus;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.*;

/** 고정 fixture만 반환한다. 모든 식별자/검색/필터/페이지 입력은 응답을 변경하지 않는다. */
public final class LockerMockData {
  public static final String DESCRIPTION = "RTR-382 고정 응답 MOCK. DB 조회/저장 및 알림 발송 없음. "
      + "요청의 식별자/검색/필터/페이지 값은 예시 응답에 반영되지 않으며, 처리 후 재조회해도 상태가 바뀌지 않습니다.";
  public static final UUID ZONE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  public static final UUID LOCKER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  public static final UUID RENTAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  public static final UUID EXTENSION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  public static final String PUBLIC_ID = "mock-locker-rental-requested";
  public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 10, 0);
  public static final LocalDate START = LocalDate.of(2026, 11, 12);
  public static final LocalDate DUE = START.plusDays(3);

  private LockerMockData() {}

  private static UUID id(String prefix, int index) {
    return UUID.fromString(prefix + "0000000-0000-0000-0000-" + String.format("%012d", index));
  }

  public static Zone zone() { return new Zone(ZONE_ID, "4층", 2, 3, 6, 2); }
  public static Zones zones() {
    return new Zones(List.of(zone(), new Zone(id("1", 2), "5층 A", 1, 2, 2, 2)));
  }
  public static Policy policy() {
    return new Policy(3, 2, 7,
        "대여 및 반납 안내를 확인하고 동의합니다. 연체 시 반납 안내가 발송됩니다.",
        1, "학생증 또는 신분증", List.of(
            new BorrowerField("department", "학과", true, 0),
            new BorrowerField("studentId", "학번", true, 1)));
  }
  public static PublicLocker locker(int index, LockerStatus status) {
    return new PublicLocker(id("2", index), String.valueOf(index), (index - 1) / 3,
        (index - 1) % 3, null, status, status == LockerStatus.RENTED);
  }
  public static Layout layout() {
    return new Layout(zone(), List.of(locker(1, LockerStatus.PENDING),
        locker(2, LockerStatus.RENTED), locker(3, LockerStatus.AVAILABLE),
        locker(4, LockerStatus.UNAVAILABLE), locker(5, LockerStatus.AVAILABLE),
        locker(6, LockerStatus.RENTED)));
  }
  public static RentalForm form() {
    return new RentalForm("건국대학교 도서관자치위원회", zone(),
        locker(3, LockerStatus.AVAILABLE), policy(), START, DUE);
  }
  public static Borrower borrower() {
    return new Borrower("홍길동", "01000000000", "borrower@example.com",
        Map.of("department", "컴퓨터공학과", "studentId", "20260000"), "반납기한 연장 요청");
  }
  public static Created created() {
    return new Created(RENTAL_ID, PUBLIC_ID, LockerRentalStatus.REQUESTED, NOW, NOW.plusMinutes(15));
  }
  public static Overview overview() {
    var lockers = layout().lockers().stream().map(locker -> {
      boolean occupied = locker.status() == LockerStatus.PENDING || locker.status() == LockerStatus.RENTED;
      boolean rented = locker.status() == LockerStatus.RENTED;
      return new ManagedLocker(locker, occupied ? id("3", Integer.parseInt(locker.label())) : null,
          occupied ? borrower() : null, rented ? START : null, rented ? DUE : null, false);
    }).toList();
    return new Overview(zones().zones(), zone(), new Summary(6, 1, 2, 2, 1), lockers);
  }
  public static ExtensionCreated extensionCreated() {
    return new ExtensionCreated(EXTENSION_ID, "mock-locker-rental-2", LockerExtensionStatus.REQUESTED,
        NOW.plusDays(1), DUE, DUE.plusDays(7));
  }
  public static Rental rental(int index, LockerRentalStatus status) {
    boolean approved = status == LockerRentalStatus.APPROVED || status == LockerRentalStatus.RETURNED;
    boolean rejected = status == LockerRentalStatus.REJECTED;
    boolean returned = status == LockerRentalStatus.RETURNED;
    return new Rental(id("3", index), index == 1 ? PUBLIC_ID : "mock-locker-rental-" + index,
        id("2", index), ZONE_ID, "4층", String.valueOf(index), status, borrower(),
        NOW, NOW.plusMinutes(15), approved ? START : null, approved ? DUE : null,
        false, 0, approved ? NOW.plusMinutes(1) : null, approved ? "김관리" : null,
        rejected ? NOW.plusMinutes(1) : null, rejected ? "김관리" : null,
        rejected ? "대여 조건 미충족" : null, returned ? NOW.plusDays(2) : null,
        returned ? "김관리" : null);
  }
  public static Rental rental() { return rental(1, LockerRentalStatus.REQUESTED); }
  public static Rentals rentals() {
    return new Rentals(List.of(rental(), rental(2, LockerRentalStatus.APPROVED),
        rental(3, LockerRentalStatus.REJECTED), rental(4, LockerRentalStatus.RETURNED)), 0, 20, 4, 1);
  }
  public static ExtensionForm extensionForm() {
    return new ExtensionForm(id("3", 2), id("2", 2), "4층", "2", START, DUE,
        DUE.plusDays(7), 0, 2, policy().borrowerFields());
  }
  public static Extension extension(int index, LockerExtensionStatus status) {
    return new Extension(id("4", index), id("3", 2), "mock-locker-rental-2", "4층", "2",
        borrower(), status, NOW.plusDays(1), DUE, DUE.plusDays(7), index,
        status == LockerExtensionStatus.APPROVED ? NOW.plusDays(1).plusMinutes(1) : null,
        status == LockerExtensionStatus.APPROVED ? "김관리" : null,
        status == LockerExtensionStatus.REJECTED ? NOW.plusDays(1).plusMinutes(1) : null,
        status == LockerExtensionStatus.REJECTED ? "김관리" : null,
        status == LockerExtensionStatus.REJECTED ? "연장 조건 미충족" : null);
  }
  public static Extension extension() { return extension(1, LockerExtensionStatus.REQUESTED); }
  public static Extensions extensions() {
    return new Extensions(List.of(extension()), 0, 20, 1, 1);
  }
  public static Action rentalAction(LockerRentalStatus status) { return new Action(RENTAL_ID, status.name(), NOW.plusMinutes(1)); }
  public static Action extensionAction(String status) { return new Action(EXTENSION_ID, status, NOW.plusDays(1).plusMinutes(1)); }
  public static BatchAction batch(LockerRentalStatus status) { return new BatchAction(List.of(rentalAction(status))); }
}
