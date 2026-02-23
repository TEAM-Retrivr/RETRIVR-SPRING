package retrivr.retrivrspring.infrastructure.repository.rental;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalRepository extends JpaRepository<Rental, Long>, RentalSearchRepository {

}
