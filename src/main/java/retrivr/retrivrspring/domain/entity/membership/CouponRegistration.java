package retrivr.retrivrspring.domain.entity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "coupon_registration",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_coupon_registration_org_coupon",
            columnNames = {"organization_id", "coupon_id"}
        )
    }
)
public class CouponRegistration extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization; // 어떤 조직이 등록했는지

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id", nullable = false)
  private Coupon coupon;  // 어떤 쿠폰인지

  private LocalDateTime registeredAt; // 등록된 시간

  public static CouponRegistration register(Organization organization, Coupon coupon, LocalDateTime now) {
    return CouponRegistration.builder()
        .organization(organization)
        .coupon(coupon)
        .registeredAt(now)
        .build();
  }

  public void validateOwner(Organization organization) {
    if (!Objects.equals(organization.getId(), this.organization.getId())) {
      throw new DomainException(ErrorCode.COUPON_OWNER_MISMATCH);
    }
  }
}
