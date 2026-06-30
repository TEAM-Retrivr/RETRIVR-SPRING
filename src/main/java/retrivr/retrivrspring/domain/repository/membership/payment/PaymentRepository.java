package retrivr.retrivrspring.domain.repository.membership.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.membership.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
