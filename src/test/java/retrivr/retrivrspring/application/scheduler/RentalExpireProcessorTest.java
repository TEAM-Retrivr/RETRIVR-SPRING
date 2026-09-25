package retrivr.retrivrspring.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import retrivr.retrivrspring.application.service.admin.rental.AdminRequestedRentalService;
import retrivr.retrivrspring.application.service.support.RentalItemLockService;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;

class RentalExpireProcessorTest {
  @Test
  void locksAllBatchItemsBeforeRejectingAnyRental() {
    RentalRepository repository = mock(RentalRepository.class);
    AdminRequestedRentalService service = mock(AdminRequestedRentalService.class);
    RentalItemLockService locks = mock(RentalItemLockService.class);
    RentalExpireProcessor processor = new RentalExpireProcessor(15, repository, service, locks);
    Rental first = expiredRental();
    Rental second = expiredRental();
    List<Long> ids = List.of(1L, 2L);
    List<Rental> rentals = List.of(first, second);
    when(repository.findExpiredRequestedIdsForUpdateSkipLocked(any(), eq(10))).thenReturn(ids);
    when(repository.findFetchBorrowerAndRentalItemAndOrganizationAllById(ids)).thenReturn(rentals);

    assertThat(processor.expireBatch(10)).isEqualTo(2);

    InOrder order = inOrder(repository, locks, service);
    order.verify(repository).findExpiredRequestedIdsForUpdateSkipLocked(any(), eq(10));
    order.verify(repository).findFetchBorrowerAndRentalItemAndOrganizationAllById(ids);
    order.verify(locks).lockItems(rentals);
    order.verify(service).rejectRentalRequestBySystem(first);
    order.verify(service).rejectRentalRequestBySystem(second);
  }

  private Rental expiredRental() {
    Rental rental = mock(Rental.class);
    when(rental.getStatus()).thenReturn(RentalStatus.REQUESTED);
    when(rental.getRequestedAt()).thenReturn(LocalDateTime.now().minusHours(1));
    return rental;
  }
}
