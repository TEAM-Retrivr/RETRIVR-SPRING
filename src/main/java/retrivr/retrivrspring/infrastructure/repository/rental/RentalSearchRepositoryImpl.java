package retrivr.retrivrspring.infrastructure.repository.rental;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
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

  @Override
  public Optional<Rental> findByIdWithItems(Long rentalId) {
    QRental rental = QRental.rental;
    QRentalItem rentalItem = QRentalItem.rentalItem;
    QRentalItemUnit rentalItemUnit = QRentalItemUnit.rentalItemUnit;
    QItem item = QItem.item;
    QItemUnit itemUnit = QItemUnit.itemUnit;

    // =========================
    // 1단계: Rental 단건만 가져오기
    //  - 여기서는 "단건 연관"만 fetch join 하자 (organization, borrower 등)
    //  - 컬렉션(rentalItems, rentalItemUnits)은 절대 fetch join X
    // =========================
    Rental foundRental = jpaQueryFactory
        .selectFrom(rental)
        .leftJoin(rental.organization).fetchJoin()
        .where(rental.id.eq(rentalId))
        .fetchOne();

    if (foundRental == null) {
      return Optional.empty();
    }

    // =========================
    // 2단계-A: rentalItems + item fetch join
    //  - 이 쿼리의 결과는 반환하지 않아도 됨 (영속성 컨텍스트에 컬렉션 채워짐)
    // =========================
    jpaQueryFactory
        .selectFrom(rentalItem)
        .join(rentalItem.rental, rental)
        .join(rentalItem.item, item).fetchJoin()
        .where(rental.id.eq(rentalId))
        .fetch();

    // =========================
    // 2단계-B: rentalItemUnits + itemUnit fetch join
    //  - 중간테이블 엔티티(RentalItemUnit)가 있을 때의 정석
    // =========================
    jpaQueryFactory
        .selectFrom(rentalItemUnit)
        .join(rentalItemUnit.rental, rental)
        .join(rentalItemUnit.itemUnit, itemUnit).fetchJoin()
        .where(rental.id.eq(rentalId))
        .fetch();

    // 이제 found.getRentalItems(), found.getRentalItemUnits() 접근해도
    // 추가 쿼리 안 나가거나(이미 로딩됨), 최소화됨.
    return Optional.of(foundRental);
  }

  @Override
  public List<Rental> searchOverduePageVerified(Long organizationId, Long cursor, int limit,
      LocalDate today) {
    QRental rental = QRental.rental;
    QRentalItem rentalItem = QRentalItem.rentalItem;
    QRentalItemUnit rentalItemUnit = QRentalItemUnit.rentalItemUnit;
    QItem item = QItem.item;
    QItemUnit itemUnit = QItemUnit.itemUnit;

    List<Rental> foundRental = jpaQueryFactory
        .selectFrom(rental)
        .join(rental.borrower).fetchJoin()
        .where(
            rental.organization.id.eq(organizationId),
            cursorLt(rental, cursor),
            rental.status.eq(RentalStatus.OVERDUE)
                .or(
                    rental.status.eq(RentalStatus.APPROVED)
                        .and(rental.dueDate.lt(today))
                )
        )
        .orderBy(rental.id.desc())
        .limit(limit)
        .fetch();

    if (foundRental.isEmpty()) {
      return foundRental;
    }

    List<Long> foundRentalIds = foundRental.stream().map(Rental::getId).toList();

    jpaQueryFactory
        .selectFrom(rentalItem)
        .join(rentalItem.item, item).fetchJoin()
        .where(rental.id.in(foundRentalIds))
        .fetch();

    jpaQueryFactory
        .selectFrom(rentalItemUnit)
        .join(rentalItemUnit.itemUnit, itemUnit).fetchJoin()
        .where(rental.id.in(foundRentalIds))
        .fetch();

    return foundRental;
  }

  @Override
  public Map<Long, Rental> findByItemUnitIn(List<ItemUnit> itemUnits) {
    if (itemUnits == null || itemUnits.isEmpty()) {
      return Map.of();
    }

    QRental rental = QRental.rental;
    QRentalItemUnit rentalItemUnit = QRentalItemUnit.rentalItemUnit;

    List<Tuple> results = jpaQueryFactory
        .select(rentalItemUnit.itemUnit.id, rental)
        .from(rentalItemUnit)
        .join(rentalItemUnit.rental, rental)
        .where(rentalItemUnit.itemUnit.in(itemUnits))
        .fetch();

    return results.stream()
        .collect(Collectors.toMap(
            tuple -> tuple.get(rentalItemUnit.itemUnit.id),
            tuple -> tuple.get(rental),
            (existing, replacement) -> existing // 혹시 중복이면 첫번째 유지
        ));
  }

  private BooleanExpression cursorLt(QRental rental, Long cursor) {
    if (cursor == null) {
      return null;
    }
    return rental.id.lt(cursor); // DESC 커서 페이징
  }

}
