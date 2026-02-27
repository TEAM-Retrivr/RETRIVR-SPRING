package retrivr.retrivrspring.infrastructure.repository.rental;

import java.util.List;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalSearchRepository {

  List<Rental> searchRequestedRentalPage(Long cursor, int limit, Long organizationId);
}
