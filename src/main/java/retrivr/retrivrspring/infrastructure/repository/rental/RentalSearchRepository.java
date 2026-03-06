package retrivr.retrivrspring.infrastructure.repository.rental;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalSearchRepository {

  List<Rental> searchRequestedRentalPage(Long cursor, int limit, Long organizationId);

  Optional<Rental> findByIdWithItems(Long rentalId);

  List<Rental> searchOverduePageVerified(Long organizationId, Long cursor, int limit,
      LocalDate today);

  Map<Long, Rental> findByItemUnitIn(List<ItemUnit> itemUnits);
}
