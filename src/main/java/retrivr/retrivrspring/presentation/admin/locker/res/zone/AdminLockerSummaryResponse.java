package retrivr.retrivrspring.presentation.admin.locker.res.zone;

import java.util.List;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerStatus;
import retrivr.retrivrspring.domain.repository.locker.LockerRepository.StatusCount;

public record AdminLockerSummaryResponse(
    long total,
    long pending,
    long available,
    long rented,
    long unavailable
) {

  public static AdminLockerSummaryResponse from(List<StatusCount> counts) {
    long total = 0, pending = 0, available = 0, rented = 0, unavailable = 0;
    for (StatusCount count : counts) {
      long quantity = count.getQuantity();
      total += quantity;
      switch (LockerStatus.from(count.getEnableStatus(), count.getOperationStatus())) {
        case PENDING -> pending += quantity;
        case AVAILABLE -> available += quantity;
        case RENTED -> rented += quantity;
        case UNAVAILABLE -> unavailable += quantity;
      }
    }
    return new AdminLockerSummaryResponse(total, pending, available, rented, unavailable);
  }
}
