package retrivr.retrivrspring.domain.repository.membership.payment;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.id = :paymentId")
  Optional<Payment> findWithLockById(String paymentId);

  Optional<Payment> findByIdAndStatus(String id, PaymentStatus status);
}
