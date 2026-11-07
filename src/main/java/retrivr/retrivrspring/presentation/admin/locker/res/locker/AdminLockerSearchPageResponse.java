package retrivr.retrivrspring.presentation.admin.locker.res.locker;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.LockerRental;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerStatus;

public record AdminLockerSearchPageResponse(
    List<LockerSearchSummary> lockers,
    Double nextScoreCursor,
    UUID nextLockerIdCursor
) {

  // TODO: 검색 응답 명세 확정 후 필드 조정
  public record LockerSearchSummary(
      UUID zoneId,
      String zoneName,
      AdminLockerResponse locker,
      LockerStatus status,
      RentalSummary rental
  ) {
    public static LockerSearchSummary from(Locker locker, LockerRental rental) {
      return new LockerSearchSummary(locker.getZone().getId(), locker.getZone().getName(),
          AdminLockerResponse.from(locker),
          LockerStatus.from(locker.getEnableStatus(), locker.getOperationStatus()),
          rental == null ? null : RentalSummary.from(rental));
    }
  }

  public record RentalSummary(
      UUID rentalId,
      String borrowerName,
      String borrowerPhone,
      String borrowerEmail,
      LocalDate dueDate
  ) {
    public static RentalSummary from(LockerRental rental) {
      return new RentalSummary(rental.getId(), rental.getBorrower().getName(),
          rental.getBorrower().getPhone(), rental.getBorrower().getEmail(), rental.getDueDate());
    }
  }
}
