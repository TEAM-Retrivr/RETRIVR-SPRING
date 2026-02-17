package retrivr.retrivrspring.infrastructure.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.application.vo.OrganizationSearchResultWithRank;
import retrivr.retrivrspring.domain.entity.organization.Organization;

@Repository
@RequiredArgsConstructor
public class OrganizationSearchRepositoryImpl implements OrganizationSearchRepository {

  private final JPAQueryFactory queryFactory;
  private final EntityManager em;
  private final float MIN_SIM_RATE = 0.2f;

  @Override
  public List<OrganizationSearchResultWithRank> searchRankedFirstPageByKeyword(String keyword, int limit) {
    String sqlNoCursor = """
        with ranked as (
          select
            o.organization_id as id,
            case
              when lower(o.name) = lower(:keyword) then 0
              when lower(o.name) like lower(:keyword || '%%') then 1
              when o.name ilike ('%%' || :keyword || '%%') then 2
              else 3
            end as bucket,
            round(similarity(o.name, :keyword)::numeric, 6)::double precision as sim
          from organization o
          where o.status = 'ACTIVE'
            and (
              o.name ilike ('%%' || :keyword || '%%')
              or similarity(o.name, :keyword) >= :minSim
            )
        )
        select r.id, r.bucket, r.sim
        from ranked r
        order by r.bucket asc, r.sim desc, r.id desc
        limit :limit
        """;
    Query q = em.createNativeQuery(sqlNoCursor)
        .setParameter("keyword", keyword)
        .setParameter("minSim", MIN_SIM_RATE) // 튜닝 포인트
        .setParameter("limit", limit);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();

    if (rows.isEmpty()) {
      return List.of();
    }

    // 1) id 순서를 보존해야 하므로 id 리스트 추출
    List<Long> idsInOrder = rows.stream()
        .map(r -> ((Number) r[0]).longValue())
        .toList();

    // 2) 엔티티 로딩 (in 절)
    List<Organization> orgs = em.createQuery("""
              select o from Organization o
              where o.id in :ids
            """, Organization.class)
        .setParameter("ids", idsInOrder)
        .getResultList();

    Map<Long, Organization> orgMap = orgs.stream()
        .collect(Collectors.toMap(Organization::getId, o -> o));

    // 3) rows 순서대로 OrganizationRankRow 조립
    List<OrganizationSearchResultWithRank> result = new ArrayList<>(rows.size());
    for (Object[] r : rows) {
      long newOrganizationId = ((Number) r[0]).longValue();
      int newBucket = ((Number) r[1]).intValue();
      double newSim = ((Number) r[2]).doubleValue();

      Organization org = orgMap.get(newOrganizationId);
      if (org != null) {
        result.add(new OrganizationSearchResultWithRank(org, newBucket, newSim));
      }
    }

    return result;
  }

  @Override
  public List<OrganizationSearchResultWithRank> searchRankedNextPageByKeyword(String keyword,
      int bucket, double sim, long organizationId, int limit) {
    String sqlWithCursor = """
          with ranked as (
            select
              o.organization_id as id,
              case
                when lower(o.name) = lower(:keyword) then 0
                when lower(o.name) like lower(:keyword || '%%') then 1
                when o.name ilike ('%%' || :keyword || '%%') then 2
                else 3
              end as bucket,
              round(similarity(o.name, :keyword)::numeric, 6)::double precision as sim
            from organization o
            where o.status = 'ACTIVE'
              and (
                o.name ilike ('%%' || :keyword || '%%')
                or similarity(o.name, :keyword) >= :minSim
              )
          )
          select r.id, r.bucket, r.sim
          from ranked r
          where
            (
              :cursorBucket is null
              or r.bucket > :cursorBucket
              or (r.bucket = :cursorBucket and r.sim < :cursorSim)
              or (r.bucket = :cursorBucket and r.sim = :cursorSim and r.id < :cursorId)
            )
          order by r.bucket asc, r.sim desc, r.id desc
          limit :limit
        """;

    Query q = em.createNativeQuery(sqlWithCursor)
        .setParameter("keyword", keyword)
        .setParameter("minSim", MIN_SIM_RATE) // 튜닝 포인트
        .setParameter("limit", limit)
        .setParameter("cursorBucket", bucket)
        .setParameter("cursorSim", sim)
        .setParameter("cursorId", organizationId);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();

    if (rows.isEmpty()) {
      return List.of();
    }

    // 1) id 순서를 보존해야 하므로 id 리스트 추출
    List<Long> idsInOrder = rows.stream()
        .map(r -> ((Number) r[0]).longValue())
        .toList();

    // 2) 엔티티 로딩 (in 절)
    List<Organization> orgs = em.createQuery("""
              select o from Organization o
              where o.id in :ids
            """, Organization.class)
        .setParameter("ids", idsInOrder)
        .getResultList();

    Map<Long, Organization> orgMap = orgs.stream()
        .collect(Collectors.toMap(Organization::getId, o -> o));

    // 3) rows 순서대로 OrganizationRankRow 조립
    List<OrganizationSearchResultWithRank> result = new ArrayList<>(rows.size());
    for (Object[] r : rows) {
      long newOrganizationId = ((Number) r[0]).longValue();
      int newBucket = ((Number) r[1]).intValue();
      double newSim = ((Number) r[2]).doubleValue();

      Organization org = orgMap.get(newOrganizationId);
      if (org != null) {
        result.add(new OrganizationSearchResultWithRank(org, newBucket, newSim));
      }
    }

    return result;
  }
/*
  @Override
  public List<Organization> searchByKeyword(String keyword, Long cursor, int limit) {
    QOrganization org = QOrganization.organization;

    return queryFactory
        .selectFrom(org)
        .where(
            org.status.eq(OrganizationStatus.ACTIVE),
            nameContainsIgnoreCase(org, keyword),
            cursorLt(org, cursor)
        )
        .orderBy(org.id.desc()) //최신 데이터부터 조회
        .limit(limit)
        .fetch();
  }

  private BooleanExpression nameContainsIgnoreCase(QOrganization o, String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return null;
    }
    // 대소문자에 상관 없이 특정 문자열이 포함되어 있는지 확인
    return o.name.containsIgnoreCase(keyword);
  }

  private BooleanExpression cursorLt(QOrganization o, Long cursor) {
    if (cursor == null) {
      return null;
    }
    return o.id.lt(cursor); // DESC 커서 페이징
  }

 */
}
