package retrivr.retrivrspring.application.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.service.admin.rental.AdminRequestedRentalService;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;

@Service
public class RentalExpireProcessor {

  private final long REQUEST_EXPIRE_MINUTES;
  private final RentalRepository rentalRepository;
  private final AdminRequestedRentalService adminRequestedRentalService;

  public RentalExpireProcessor(
      @Value("${scheduler.rental-expire.request-expire-minutes}")
      int requestExpireMinutes,
      RentalRepository rentalRepository,
      AdminRequestedRentalService adminRequestedRentalService
  ) {
    this.rentalRepository = rentalRepository;
    this.REQUEST_EXPIRE_MINUTES = requestExpireMinutes;
    this.adminRequestedRentalService = adminRequestedRentalService;
  }

  @Transactional
  public int expireBatch(int batchSize) {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(REQUEST_EXPIRE_MINUTES);

    List<Long> rentalIds = rentalRepository.findExpiredRequestedIdsForUpdateSkipLocked(threshold, batchSize);
    List<Rental> rentals = rentalRepository.findAllById(rentalIds);
    int rejectedCount = 0;

    for (Rental rental : rentals) {
      rejectedCount += expireOne(rental);
    }

    return rejectedCount;
  }

  private int expireOne(Rental rental) {
    if (rental.getStatus() != RentalStatus.REQUESTED) {
      return 0;
    }
    if (!isExpired(rental, LocalDateTime.now())) {
      return 0;
    }

    adminRequestedRentalService.rejectRentalRequestBySystem(rental);
    return 1;
  }

  private boolean isExpired(Rental rental, LocalDateTime now) {
    return !rental.getRequestedAt().plusMinutes(REQUEST_EXPIRE_MINUTES).isAfter(now);
  }
}
