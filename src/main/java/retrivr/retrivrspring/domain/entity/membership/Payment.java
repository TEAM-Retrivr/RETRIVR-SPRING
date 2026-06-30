package retrivr.retrivrspring.domain.entity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "payment"
)
public class Payment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id")
  private Subscription subscription;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionPlan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status; // SUCCESS, FAILED

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider; // MOCK, TOSS

  @Column(nullable = false)
  private Long amount;

  @Column
  private String providerPaymentKey;

  @Column
  private String failureCode;

  @Column
  private String failureReason;

  @Column
  private LocalDateTime paidAt;

  @Column
  private LocalDateTime failedAt;

  public static Payment success(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      PaymentProvider provider,
      LocalDateTime paidAt
  ) {
    return Payment.builder()
        .organization(organization)
        .subscription(subscription)
        .plan(plan)
        .status(PaymentStatus.SUCCESS)
        .provider(provider)
        .amount((long) plan.getPrice())
        .providerPaymentKey(provider.name().toLowerCase() + "_" + UUID.randomUUID())
        .paidAt(paidAt)
        .build();
  }

  public static Payment fail(
      Organization organization,
      Subscription subscription,
      SubscriptionPlan plan,
      PaymentProvider provider,
      String failureCode,
      String failureReason,
      LocalDateTime failedAt
  ) {
    return Payment.builder()
        .organization(organization)
        .subscription(subscription)
        .plan(plan)
        .status(PaymentStatus.FAILED)
        .provider(provider)
        .amount((long) plan.getPrice())
        .failureCode(failureCode)
        .failureReason(failureReason)
        .failedAt(failedAt)
        .build();
  }

  public boolean isSuccess() {
    return this.status == PaymentStatus.SUCCESS;
  }

  public boolean isFailed() {
    return this.status == PaymentStatus.FAILED;
  }
}
