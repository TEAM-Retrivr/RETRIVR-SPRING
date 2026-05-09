package retrivr.retrivrspring.domain.repository.membership.coupon;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import retrivr.retrivrspring.domain.entity.membership.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, String> {

  Optional<Coupon> findByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Coupon c where c.id = :couponId")
  Optional<Coupon> findByIdForUpdate(String couponId);

  boolean existsByCode(String code);
}
