package retrivr.retrivrspring.domain.repository.membership.coupon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.infrastructure.repository.membership.coupon.CouponRepositoryCustom;

public interface CouponRepository extends JpaRepository<Coupon, String>, CouponRepositoryCustom {

  Optional<Coupon> findByCode(String code);

  boolean existsByCode(String code);
}
