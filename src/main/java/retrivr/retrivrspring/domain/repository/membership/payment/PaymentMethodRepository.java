package retrivr.retrivrspring.domain.repository.membership.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.membership.PaymentMethod;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentMethodStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {

  boolean existsByOrganization(Organization organization);

  List<PaymentMethod> findAllByOrganizationOrderByRegisteredAtDesc(Organization organization);

  @Query("""
      select pm
      from PaymentMethod pm
      where pm.organization = :organization
        and pm.isDefault = true
      """)
  List<PaymentMethod> findDefaultPaymentMethods(@Param("organization") Organization organization);

  @Query("""
      select pm
      from PaymentMethod pm
      where pm.organization = :organization
        and pm.isDefault = true
        and pm.status = :status
      """)
  Optional<PaymentMethod> findDefaultPaymentMethod(
      @Param("organization") Organization organization,
      @Param("status") PaymentMethodStatus status
  );

  Optional<PaymentMethod> findByIdAndOrganization(String id, Organization organization);
}
