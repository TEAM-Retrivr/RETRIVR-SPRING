package retrivr.retrivrspring.application.service.rental;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import retrivr.retrivrspring.application.service.support.RentalItemLockService;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentalItem;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

class RentalItemLockServiceTest {
  private final ItemRepository repository = mock(ItemRepository.class);
  private final RentalItemLockService service = new RentalItemLockService(repository);

  @Test
  void locksWholeBatchInAscendingOrderWithoutDuplicateLocks() {
    Item first = Item.builder().id(1L).build();
    Item second = Item.builder().id(2L).build();
    when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(first));
    when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(second));

    service.lockItems(List.of(rental(second), rental(first), rental(second)));

    InOrder order = inOrder(repository);
    order.verify(repository).findByIdForUpdate(1L);
    order.verify(repository).findByIdForUpdate(2L);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void missingItemStopsProcessing() {
    when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.lockItems(List.of(rental(Item.builder().id(1L).build()))))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND_ITEM);
  }

  @Test
  void emptyBatchDoesNotAcquireLocks() {
    service.lockItems(List.of());
    verifyNoInteractions(repository);
  }

  private Rental rental(Item item) {
    Rental rental = mock(Rental.class);
    when(rental.getRentalItems()).thenReturn(List.of(RentalItem.create(rental, item)));
    return rental;
  }
}
