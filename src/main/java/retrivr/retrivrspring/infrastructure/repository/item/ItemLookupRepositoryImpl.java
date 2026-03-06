package retrivr.retrivrspring.infrastructure.repository.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.QItem;

@Repository
@RequiredArgsConstructor
public class ItemLookupRepositoryImpl implements ItemLookupRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Item> findPageByOrganizationWithCursor(Long organizationId, Long cursor, int limit) {
    QItem item = QItem.item;

    return queryFactory
        .selectFrom(item)
        .where(
            item.organization.id.eq(organizationId),
            cursorLt(item, cursor)
        )
        .orderBy(item.id.desc())
        .limit(limit)
        .fetch();
  }

  private BooleanExpression cursorLt(QItem item, Long cursor) {
    if (cursor == null) {
      return null;
    }
    return item.id.lt(cursor); // DESC 커서 페이징
  }

}
