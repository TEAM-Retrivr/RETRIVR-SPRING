package retrivr.retrivrspring.domain.repository.membership.payment;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public interface PaymentRepository extends JpaRepository<Payment, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.id = :paymentId")
  Optional<Payment> findWithLockById(@Param("paymentId") String paymentId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.id = :paymentId and p.status = :status")
  Optional<Payment> findByIdAndStatus(
      @Param("paymentId") String paymentId,
      @Param("status") PaymentStatus status
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.organization = :organization and p.status = :status")
  Optional<Payment> findByOrganizationAndStatus(
      @Param("organization") Organization organization,
      @Param("status") PaymentStatus status
  );

  Optional<Payment> findFirstByOrganizationAndStatusInOrderByCreatedAtDesc(
      Organization organization,
      List<PaymentStatus> statuses
  );

  @Query("""
      select p.id
      from Payment p
      where p.status = :status
        and p.failedAt <= :retryBefore
      order by p.failedAt asc
      """)
  List<String> findIdsForReconciliation(
      @Param("status") PaymentStatus status,
      @Param("retryBefore") LocalDateTime retryBefore,
      Pageable pageable
  );

  @Query("""
      select p.id
      from Payment p
      where p.status in :statuses
        and p.failedAt <= :retryBefore
      order by p.failedAt asc
      """)
  List<String> findIdsForCompensation(
      @Param("statuses") List<PaymentStatus> statuses,
      @Param("retryBefore") LocalDateTime retryBefore,
      Pageable pageable
  );

  @Query("""
      select p.id
      from Payment p
      where p.status in :statuses
        and p.failedAt <= :retryBefore
      order by p.failedAt asc
      """)
  List<String> findIdsForScheduleRetry(
      @Param("statuses") List<PaymentStatus> statuses,
      @Param("retryBefore") LocalDateTime retryBefore,
      Pageable pageable
  );

  @Query("""
      select p.id
      from Payment p
      where p.status = :status
        and p.createdAt <= :retryBefore
      order by p.createdAt asc
      """)
  List<String> findPendingIdsForRecovery(
      @Param("status") PaymentStatus status,
      @Param("retryBefore") LocalDateTime retryBefore,
      Pageable pageable
  );
}
