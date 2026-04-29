package retrivr.retrivrspring.infrastructure.repository.rental;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.application.vo.RentedRentalSearchResultWithScore;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.QItem;
import retrivr.retrivrspring.domain.entity.item.QItemUnit;
import retrivr.retrivrspring.domain.entity.organization.QOrganization;
import retrivr.retrivrspring.domain.entity.rental.*;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RentalSearchRepositoryImpl implements RentalSearchRepository {

  private final JPAQueryFactory jpaQueryFactory;
  private final EntityManager em;

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
    long offset = cursor != null ? cursor : 0L;

    List<Rental> foundRental = jpaQueryFactory
        .selectFrom(rental)
        .join(rental.borrower).fetchJoin()
        .where(
            rental.organization.id.eq(organizationId),
            rental.status.eq(RentalStatus.RENTED)
                .and(rental.dueDate.lt(today))
        )
        .orderBy(rental.id.desc())
        .offset(offset)
        .limit(limit)
        .fetch();

    if (foundRental.isEmpty()) {
      return foundRental;
    }

    List<Long> foundRentalIds = foundRental.stream().map(Rental::getId).toList();

    jpaQueryFactory
        .selectFrom(rentalItem)
        .join(rentalItem.rental, rental)
        .join(rentalItem.item, item).fetchJoin()
        .where(rental.id.in(foundRentalIds))
        .fetch();

    jpaQueryFactory
        .selectFrom(rentalItemUnit)
        .join(rentalItemUnit.rental, rental)
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
        .where(
            rentalItemUnit.itemUnit.in(itemUnits),
            rental.status.eq(RentalStatus.RENTED)
        )
        .fetch();

    return results.stream()
        .collect(Collectors.toMap(
            tuple -> tuple.get(rentalItemUnit.itemUnit.id),
            tuple -> tuple.get(rental),
            (existing, replacement) -> existing // 혹시 중복이면 첫번째 유지
        ));
  }

  @Override
  public List<Rental> findRentedByItemId(Long itemId, Long cursor, int limit) {
    QRental rental = QRental.rental;
    QBorrower borrower = QBorrower.borrower;
    QRentalItem rentalItem = QRentalItem.rentalItem;
    QItem item = QItem.item;

    List<Long> rentalIds = jpaQueryFactory
        .select(rental.id)
        .from(rental)
        .join(rental.rentalItems, rentalItem)
        .where(
            rentalItem.item.id.eq(itemId),
            rental.status.eq(RentalStatus.RENTED),
            cursorLt(rental, cursor)
        )
        .orderBy(rental.id.desc())
        .limit(limit)
        .fetch();

    if (rentalIds.isEmpty()) {
      return List.of();
    }

    List<Rental> rentals = jpaQueryFactory
        .selectFrom(rental)
        .distinct()
        .join(rental.borrower, borrower).fetchJoin()
        .where(rental.id.in(rentalIds))
        .orderBy(rental.id.desc())
        .fetch();

    jpaQueryFactory
        .selectFrom(rentalItem)
        .join(rentalItem.rental, rental)
        .join(rentalItem.item, item).fetchJoin()
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

  @Override
  public List<Rental> findOverdueReminderTargets(LocalDate today) {
    QRental rental = QRental.rental;
    QBorrower borrower = QBorrower.borrower;
    QOrganization organization = QOrganization.organization;
    QRentalItem rentalItem = QRentalItem.rentalItem;
    QItem item = QItem.item;

    return jpaQueryFactory
        .selectDistinct(rental)
        .from(rental)
        .join(rental.borrower, borrower).fetchJoin()
        .join(rental.organization, organization).fetchJoin()
        .join(rental.rentalItems, rentalItem).fetchJoin()
        .join(rentalItem.item, item).fetchJoin()
        .where(
            rental.returnedAt.isNull(),
            rental.dueDate.before(today),
            borrower.phone.phone.isNotNull(),
            borrower.phone.phone.ne(""),
            rental.status.eq(RentalStatus.RENTED),
            item.useMessageAlarmService.isTrue()
        )
        .fetch();
  }

  @Override
  public List<RentedRentalSearchResultWithScore> searchRentedRentalPageBy(Long organizationId, String keyword, Long cursorRentalId, Double cursorScore,
      int size) {
    Query query;
    if (cursorRentalId == null || cursorScore == null) {
      query = searchRentedRentalPageQueryIfCursorNull(organizationId, keyword, size);
    }
    else {
      query = searchRentedRentalPageQueryIfCursorExist(organizationId, keyword, cursorRentalId, cursorScore, size);
    }

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();

    if (rows.isEmpty()) {
      return List.of();
    }

    return rows.stream()
        .map(r -> new RentedRentalSearchResultWithScore((Long) r[0], (Double) r[1]))
        .toList();
  }

  private Query searchRentedRentalPageQueryIfCursorNull(Long organizationId, String keyword, int size) {
    String rawQuery = """
        with scored as (
                select
                    r.rental_id as rentalId,
                    max(
                        similarity(b.name, :keyword)
                        + similarity(i.name, :keyword)
                        + case when lower(b.name) like lower(concat('%', :keyword, '%')) then 0.8 else 0.0 end
                        + case when lower(i.name) like lower(concat('%', :keyword, '%')) then 0.8 else 0.0 end
                        + case when b.phone like concat('%', :keyword, '%') then 1.0 else 0.0
                          end
                    ) as score
                from rental r
                join borrower b on r.borrower_id = b.borrower_id
                join rental_item ri on ri.rental_id = r.rental_id
                join item i on ri.item_id = i.item_id
                where r.organization_id = :organizationId
                  and r.status = 'RENTED'
                  and :keyword is not null
                  and trim(:keyword) != ''
                  and (     
                        b.name ilike concat('%', :keyword, '%')
                        or i.name ilike concat('%', :keyword, '%')
                        or similarity(b.name, :keyword) > 0.3
                        or similarity(i.name, :keyword) > 0.3
                        or b.phone like concat('%', :keyword, '%')
                )
                group by r.rental_id
            )
            select rentalId, score
            from scored
            order by score desc, rentalId desc
            limit :size
        """;

    return em.createNativeQuery(rawQuery)
        .setParameter("keyword", keyword)
        .setParameter("size", size)
        .setParameter("organizationId", organizationId);
  }

  private Query searchRentedRentalPageQueryIfCursorExist(Long organizationId, String keyword, Long cursorRentalId, Double cursorScore,
      int size) {
    String rawQuery = """
        with scored as (
                select
                    r.rental_id as rentalId,
                    max(
                        similarity(b.name, :keyword)
                        + similarity(i.name, :keyword)
                        + case when lower(b.name) like lower(concat('%', :keyword, '%')) then 0.8 else 0.0 end
                        + case when lower(i.name) like lower(concat('%', :keyword, '%')) then 0.8 else 0.0 end
                        + case when b.phone like concat('%', :keyword, '%') then 1.0 else 0.0
                          end
                    ) as score
                from rental r
                join borrower b on r.borrower_id = b.borrower_id
                join rental_item ri on ri.rental_id = r.rental_id
                join item i on ri.item_id = i.item_id
                where r.organization_id = :organizationId
                  and r.status = 'RENTED'
                  and :keyword is not null
                  and trim(:keyword) != ''
                  and (               
                        b.name ilike concat('%', :keyword, '%')
                        or i.name ilike concat('%', :keyword, '%')
                        or similarity(b.name, :keyword) > 0.3
                        or similarity(i.name, :keyword) > 0.3
                        or b.phone like concat('%', :keyword, '%')
                )
                group by r.rental_id
            )
            select rentalId, score
            from scored
            where (
                score < :cursorScore
                or (score = :cursorScore and rentalId < :cursorRentalId)
            )
            order by score desc, rentalId desc
            limit :size
        """;
    return em.createNativeQuery(rawQuery)
        .setParameter("keyword", keyword)
        .setParameter("size", size)
        .setParameter("organizationId", organizationId)
        .setParameter("cursorScore", cursorScore)
        .setParameter("cursorRentalId", cursorRentalId);
  }
}
