package retrivr.retrivrspring.infrastructure.repository.rental;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.item.QItem;
import retrivr.retrivrspring.domain.entity.item.QItemUnit;
import retrivr.retrivrspring.domain.entity.rental.*;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RentalSearchRepositoryImpl implements RentalSearchRepository {

  private final JPAQueryFactory jpaQueryFactory;


  public List<Rental> searchRequestedRentalPage(Long cursor, int limit, Long organizationId) {
    QRental rental = QRental.rental;
    QBorrower borrower = QBorrower.borrower;
    QRentalItem rentalItem = QRentalItem.rentalItem;
    QItem item = QItem.item;
    QRentalItemUnit rentalItemUnit = QRentalItemUnit.rentalItemUnit;
    QItemUnit itemUnit = QItemUnit.itemUnit;

    List<Long> rentalIds = jpaQueryFactory
        .select(rental.id)
        .from(rental)
        .where(
            rental.status.eq(RentalStatus.REQUESTED),
            cursorLt(rental, cursor),
            rental.organization.id.eq(organizationId)
        )
        .orderBy(rental.id.desc())
        .limit(limit)
        .fetch();

    if (rentalIds.isEmpty()) return List.of();

    // A) rental + borrower
    List<Rental> rentals = jpaQueryFactory
        .selectFrom(rental)
        .distinct()
        .leftJoin(rental.borrower, borrower).fetchJoin()
        .where(rental.id.in(rentalIds))
        .orderBy(rental.id.desc())
        .fetch();

    // B) rentalItems (+ item)
    jpaQueryFactory
        .selectFrom(rental)
        .distinct()
        .leftJoin(rental.rentalItems, rentalItem).fetchJoin()
        .leftJoin(rentalItem.item, item).fetchJoin()
        .where(rental.id.in(rentalIds))
        .fetch();

    // C) rentalItemUnits (+ itemUnit)
    jpaQueryFactory
        .selectFrom(rental)
        .distinct()
        .leftJoin(rental.rentalItemUnits, rentalItemUnit).fetchJoin()
        .leftJoin(rentalItemUnit.itemUnit, itemUnit).fetchJoin()
        .where(rental.id.in(rentalIds))
        .fetch();

    return rentals;
  }

  private BooleanExpression cursorLt(QRental rental, Long cursor) {
    if (cursor == null) {
      return null;
    }
    return rental.id.lt(cursor); // DESC 커서 페이징
  }

}
