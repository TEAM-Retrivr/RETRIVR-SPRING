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
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentMethodStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_method")
public class PaymentMethod extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider;

  @Column(nullable = false, length = 255)
  private String billingKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethodStatus status;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(nullable = false)
  private LocalDateTime registeredAt;

  private LocalDateTime disabledAt;

  public static PaymentMethod register(
      Organization organization,
      PaymentProvider provider,
      String billingKey,
      boolean isDefault,
      LocalDateTime now
  ) {
    validateProvider(provider);
    validateBillingKey(billingKey);
    return PaymentMethod.builder()
        .organization(organization)
        .provider(provider)
        .billingKey(billingKey)
        .status(PaymentMethodStatus.ACTIVE)
        .isDefault(isDefault)
        .registeredAt(now)
        .build();
  }

  public void update(PaymentProvider provider, String billingKey, LocalDateTime now) {
    validateProvider(provider);
    validateBillingKey(billingKey);
    this.provider = provider;
    this.billingKey = billingKey;
    this.status = PaymentMethodStatus.ACTIVE;
    this.registeredAt = now;
    this.disabledAt = null;
  }

  public void markDefault() {
    validateActive();
    this.isDefault = true;
  }

  public void unmarkDefault() {
    this.isDefault = false;
  }

  public void disable(LocalDateTime now) {
    validateActive();
    this.status = PaymentMethodStatus.DISABLED;
    this.isDefault = false;
    this.disabledAt = now;
  }

  public void validateOwner(Organization owner) {
    if (!Objects.equals(owner.getId(), organization.getId())) {
      throw new DomainException(ErrorCode.SUBSCRIPTION_OWNER_MISMATCH);
    }
  }

  public void validateActive() {
    if (this.status != PaymentMethodStatus.ACTIVE) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION, "paymentMethod must be active");
    }
  }

  public String getBillingKeyOrThrow() {
    validateBillingKey(this.billingKey);
    return this.billingKey;
  }

  private static void validateProvider(PaymentProvider provider) {
    if (provider == null) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION, "provider must not be null");
    }
  }

  private static void validateBillingKey(String billingKey) {
    if (billingKey == null || billingKey.isBlank()) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION, "billingKey must not be blank");
    }
  }
}
