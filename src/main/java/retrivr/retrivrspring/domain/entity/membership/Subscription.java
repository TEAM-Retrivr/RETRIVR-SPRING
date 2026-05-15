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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "subscription",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_org",
            columnNames = {"organization_id"}
        )
    }
)
public class Subscription extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionPlan plan; // MONTHLY, YEARLY

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus status; // ACTIVE, CANCELED, PAYMENT_FAILED

  private LocalDateTime nextBillingAt; // 다음 결제 시각

  private LocalDateTime startedAt; // 최초 구독 시작 시각

  private LocalDateTime canceledAt; // 해지 시각

  private LocalDateTime paymentFailedAt; // 마지막 결제 실패 시각

  public void validateOwner(Organization owner) {
    if (!Objects.equals(owner.getId(), organization.getId())) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_OWNER_MISMATCH);
    }
  }

  public int getDurationDays() {
    if (this.plan == SubscriptionPlan.MONTHLY) {
      return 30;
    }
    if (this.plan == SubscriptionPlan.YEARLY) {
      return 365;
    }
    throw new DomainException(ErrorCode.INVALID_SUBSCRIPTION_PLAN);
  }

  public boolean isActive() {
    return this.status == SubscriptionStatus.ACTIVE;
  }

  public LocalDateTime getNextBillingAt() {
    if (!isActive()) {
      return null;
    }
    return this.nextBillingAt;
  }
}
