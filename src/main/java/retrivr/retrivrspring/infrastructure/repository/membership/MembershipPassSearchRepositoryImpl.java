package retrivr.retrivrspring.infrastructure.repository.membership;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.QMembershipPass;
import retrivr.retrivrspring.domain.entity.organization.Organization;

@Repository
@RequiredArgsConstructor
public class MembershipPassSearchRepositoryImpl implements MembershipPassSearchRepository {

  private final JPAQueryFactory queryFactory;


  @Override
  public List<MembershipPass> findMembershipPassHistory(
      Organization organization,
      Long cursor,
      LocalDateTime start,
      LocalDateTime end,
      Pageable pageable
  ) {
    QMembershipPass membershipPass = QMembershipPass.membershipPass;

    return queryFactory
        .selectFrom(membershipPass)
        .where(
            membershipPass.organization.eq(organization),
            sequenceLt(membershipPass, cursor),
            createdAtGoe(membershipPass, start),
            createdAtLt(membershipPass, end)
        )
        .orderBy(membershipPass.sequence.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  private BooleanExpression sequenceLt(
      QMembershipPass membershipPass,
      Long cursor
  ) {
    return cursor != null
        ? membershipPass.sequence.lt(cursor)
        : null;
  }

  private BooleanExpression createdAtGoe(
      QMembershipPass membershipPass,
      LocalDateTime start
  ) {
    return start != null
        ? membershipPass.createdAt.goe(start)
        : null;
  }

  private BooleanExpression createdAtLt(
      QMembershipPass membershipPass,
      LocalDateTime end
  ) {
    return end != null
        ? membershipPass.createdAt.lt(end)
        : null;
  }

}
