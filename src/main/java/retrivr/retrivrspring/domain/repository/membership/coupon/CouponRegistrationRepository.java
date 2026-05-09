package retrivr.retrivrspring.domain.repository.membership.coupon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface CouponRegistrationRepository extends JpaRepository<CouponRegistration, String> {

  Optional<CouponRegistration> findByOrganizationAndCoupon(Organization organization, Coupon coupon);

  boolean existsByOrganizationAndCoupon(Organization organization, Coupon coupon);
}
