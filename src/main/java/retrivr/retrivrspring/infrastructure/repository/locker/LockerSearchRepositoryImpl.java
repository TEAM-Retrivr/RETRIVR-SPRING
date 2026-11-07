package retrivr.retrivrspring.infrastructure.repository.locker;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.application.vo.LockerSearchResultWithScore;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.QLocker;

@Repository
@RequiredArgsConstructor
public class LockerSearchRepositoryImpl implements LockerSearchRepository {

  private final EntityManager em;
  private final JPAQueryFactory jpaQueryFactory;

  private static final String SCORED_QUERY = """
      with scored as (
          select
              l.id as locker_id,
              cast(max(
                  similarity(l.label, :keyword)
                  + coalesce(similarity(r.borrower_name, :keyword), 0)
                  + case when l.label ilike :pattern escape '!' then 0.8 else 0.0 end
                  + case when r.borrower_name ilike :pattern escape '!' then 0.8 else 0.0 end
                  + case when r.borrower_phone like :pattern escape '!' then 1.0 else 0.0 end
                  + case when r.borrower_email ilike :pattern escape '!' then 1.0 else 0.0 end
              ) as double precision) as score
          from locker l
          join locker_zone z on z.id = l.zone_id
          left join locker_rental r
              on r.locker_id = l.id
              and r.organization_id = :organizationId
              and r.status in ('REQUESTED', 'APPROVED')
          where z.organization_id = :organizationId
            and l.enable_status = 'ENABLED'
            and (
                l.label ilike :pattern escape '!'
                or similarity(l.label, :keyword) > 0.3
                or r.borrower_name ilike :pattern escape '!'
                or similarity(r.borrower_name, :keyword) > 0.3
                or r.borrower_phone like :pattern escape '!'
                or r.borrower_email ilike :pattern escape '!'
            )
          group by l.id
      )
      select locker_id, score
      from scored
      """;

  @Override
  public List<LockerSearchResultWithScore> searchLockerPageBy(
      Long organizationId, String keyword, UUID cursorLockerId, Double cursorScore, int size) {
    if (organizationId == null || size <= 0) {
      throw new IllegalArgumentException("기관 ID와 양수인 조회 크기가 필요합니다.");
    }
    if ((cursorLockerId == null) != (cursorScore == null)) {
      throw new IllegalArgumentException("사물함 ID 커서와 점수 커서는 함께 전달해야 합니다.");
    }
    if (cursorScore != null && (!Double.isFinite(cursorScore) || cursorScore < 0)) {
      throw new IllegalArgumentException("점수 커서는 유한한 0 이상의 값이어야 합니다.");
    }
    if (keyword == null || keyword.isBlank()) {
      return List.of();
    }

    String normalizedKeyword = keyword.strip();
    Query query = cursorLockerId == null
        ? searchLockerPageQueryIfCursorNull(organizationId, normalizedKeyword, size)
        : searchLockerPageQueryIfCursorExist(organizationId, normalizedKeyword, cursorLockerId, cursorScore, size);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();
    return rows.stream()
        .map(row -> new LockerSearchResultWithScore((UUID) row[0], ((Number) row[1]).doubleValue()))
        .toList();
  }

  private Query searchLockerPageQueryIfCursorNull(Long organizationId, String keyword, int size) {
    return createSearchQuery(SCORED_QUERY + """
        order by score desc, locker_id desc
        limit :size
        """, organizationId, keyword, size);
  }

  private Query searchLockerPageQueryIfCursorExist(
      Long organizationId, String keyword, UUID cursorLockerId, Double cursorScore, int size) {
    return createSearchQuery(SCORED_QUERY + """
        where score < :cursorScore
           or (score = :cursorScore and locker_id < :cursorLockerId)
        order by score desc, locker_id desc
        limit :size
        """, organizationId, keyword, size)
        .setParameter("cursorLockerId", cursorLockerId)
        .setParameter("cursorScore", cursorScore);
  }

  private Query createSearchQuery(String sql, Long organizationId, String keyword, int size) {
    // LIKE 와일드카드도 검색어의 문자 그대로 취급한다.
    String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    return em.createNativeQuery(sql)
        .setParameter("organizationId", organizationId)
        .setParameter("keyword", keyword)
        .setParameter("pattern", pattern)
        .setParameter("size", size);
  }

  @Override
  public boolean existsAdjacentGroups(Locker target, Locker parent) {
    QLocker targetCell = new QLocker("targetCell");
    QLocker parentCell = new QLocker("parentCell");

    BooleanExpression horizontal = targetCell.rowIndex.eq(parentCell.rowIndex)
        .and(targetCell.columnIndex.subtract(parentCell.columnIndex).abs().eq(1));
    BooleanExpression vertical = targetCell.columnIndex.eq(parentCell.columnIndex)
        .and(targetCell.rowIndex.subtract(parentCell.rowIndex).abs().eq(1));

    return jpaQueryFactory.selectOne()
        .from(targetCell, parentCell)
        .where(
            targetCell.parentLocker.eq(target),
            parentCell.parentLocker.eq(parent),
            targetCell.zone.eq(parentCell.zone),
            horizontal.or(vertical)
        )
        .fetchFirst() != null;
  }
}
