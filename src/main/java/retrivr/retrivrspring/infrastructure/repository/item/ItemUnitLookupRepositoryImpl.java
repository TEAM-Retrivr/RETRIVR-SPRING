package retrivr.retrivrspring.infrastructure.repository.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.QItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;

@Repository
@RequiredArgsConstructor
public class ItemUnitLookupRepositoryImpl implements ItemUnitLookupRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<ItemUnit> searchRentedUnitsByItemId(Long itemId, Long cursor, int limit) {
    QItemUnit unit = QItemUnit.itemUnit;

    return queryFactory
        .selectFrom(unit)
        .where(
            unit.item.id.eq(itemId),
            unit.status.eq(ItemUnitStatus.RENTED),
            cursorLt(unit, cursor)
        )
        .orderBy(unit.id.desc())
        .limit(limit)
        .fetch();

  }

  private BooleanExpression cursorLt(QItemUnit itemUnit, Long cursor) {
    if (cursor == null) {
      return null;
    }
    return itemUnit.id.lt(cursor); // DESC 커서 페이징
  }
}
