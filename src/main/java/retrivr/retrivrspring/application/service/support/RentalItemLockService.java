package retrivr.retrivrspring.application.service.support;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class RentalItemLockService {
  private final ItemRepository itemRepository;

  /**
   * 호출자는 Rental 락을 먼저 획득해야 한다. Item 상태 및 유닛은 이 메서드 이후 읽는다.
   * 배치에서는 전체 대상의 Item을 미리 정렬하여 잠그고, 이후 기존 Rental 락을 추가하지 않는다.
   */
  public void lockItems(Collection<Rental> rentals) {
    rentals.stream()
        .flatMap(rental -> rental.getRentalItems().stream())
        .map(rentalItem -> rentalItem.getItem().getId())
        .distinct()
        .sorted()
        .forEach(itemId -> itemRepository.findByIdForUpdate(itemId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM)));
  }
}
