package retrivr.retrivrspring.infrastructure.repository.membership.coupon;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.QCoupon;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryCustomImpl implements CouponRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public boolean consumeIfAvailable(Coupon coupon, LocalDate today) {

    QCoupon qCoupon = QCoupon.coupon;

    long updatedCount = queryFactory
        .update(qCoupon)
        .set(
            qCoupon.usedQuantity,
            qCoupon.usedQuantity.add(1)
        )
        .where(
            qCoupon.id.eq(coupon.getId()),
            qCoupon.usedQuantity.lt(qCoupon.totalQuantity),
            qCoupon.expiresAt.goe(today)
        )
        .execute();

    return updatedCount == 1;
  }
}
