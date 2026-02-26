package retrivr.retrivrspring.infrastructure.repository.rental;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

import java.util.List;
import java.util.Optional;

public interface RentalRepository
        extends JpaRepository<Rental, Long>, RentalSearchRepository {

    @EntityGraph(attributePaths = {"rentalItems", "organization"})
    Optional<Rental> findFetchRentalItemAndOrganizationById(Long rentalId);

    // 전체 REQUESTED 개수
    int countByOrganization_IdAndStatus(Long organizationId, RentalStatus status);

    // 최신 2건 Projection
    @Query("""
                select distinct r
                from Rental r
                join fetch r.borrower
                join fetch r.rentalItems ri
                join fetch ri.item
                where r.organization.id = :organizationId
                  and r.status = :status
                order by r.requestedAt desc
            """)
    List<Rental> findRecentHomeRentals(
            @Param("organizationId") Long organizationId,
            @Param("status") RentalStatus status,
            Pageable pageable
    );
}