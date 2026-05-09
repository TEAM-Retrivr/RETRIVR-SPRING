package retrivr.retrivrspring.domain.entity.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.membership.enumerate.CouponStatus;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "coupon",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_coupon_code",
            columnNames = {"code"}
        )
    }
)
public class Coupon extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String code; // 쿠폰 코드

  @Column(nullable = false)
  private String name; // 쿠폰 이름

  @Column(nullable = false)
  private String guideline; // 안내 사항

  @Column(nullable = false)
  private String description; // 쿠폰 설명

  @Column(nullable = false)
  private int totalQuantity; // 쿠폰 총 수량

  @Column(nullable = false)
  private int usedQuantity; // 쿠폰 사용된 수량

  @Column(nullable = false)
  private int durationDays; // 쿠폰 사용 기간

  private LocalDate activeStartAt; // 쿠폰 활성화 시작일

  private LocalDate expiresAt; // 쿠폰 만료일

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CouponStatus status; // ACTIVE, DISABLED, EXPIRED

  public static Coupon create(String code, String name, String guideline, String description,
      int totalQuantity, int durationDays, LocalDate activeStartAt, LocalDate expiresAt) {
    return Coupon.builder()
        .code(code)
        .name(name)
        .guideline(guideline)
        .description(description)
        .totalQuantity(totalQuantity)
        .usedQuantity(0)
        .durationDays(durationDays)
        .activeStartAt(activeStartAt)
        .expiresAt(expiresAt)
        .status(CouponStatus.ACTIVE)
        .build();
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt != null && expiresAt.isBefore(LocalDate.from(now));
  }

  public boolean isActive(LocalDateTime now) {
    if (status != CouponStatus.ACTIVE) {
      return false;
    }
    if (isExpired(now)) {
      return false;
    }
    return activeStartAt != null && !activeStartAt.isAfter(LocalDate.from(now));
  }

  public boolean isQuantityExceeded() {
    return usedQuantity >= totalQuantity;
  }

  public boolean isAvailable(LocalDateTime now) {
    return isActive(now) && !isQuantityExceeded();
  }

  public boolean registered(LocalDateTime now) {
    if (!isAvailable(now)) {
      return false;
    }

    usedQuantity++;
    return true;
  }
}
