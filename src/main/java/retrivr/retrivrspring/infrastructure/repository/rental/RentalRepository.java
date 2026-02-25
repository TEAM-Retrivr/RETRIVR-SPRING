package retrivr.retrivrspring.infrastructure.repository.rental;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public interface RentalRepository extends JpaRepository<Rental, Long>, RentalSearchRepository {

  @EntityGraph(attributePaths = {"rentalItems", "organization"})
  Optional<Rental> findFetchRentalItemAndOrganizationById(Long rentalId);
}
