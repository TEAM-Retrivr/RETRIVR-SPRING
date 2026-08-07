package retrivr.retrivrspring.presentation.admin.membership.pass.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import retrivr.retrivrspring.domain.entity.membership.Coupon;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;

@Schema(description = "사용 중이거나 사용 대기 중인 쿠폰 이용권 목록 응답")
public record CouponMembershipPassListResponse(
    List<CouponMembershipPassResponse> coupons
) {

  public static CouponMembershipPassListResponse from(List<MembershipPass> membershipPasses) {
    return new CouponMembershipPassListResponse(
        membershipPasses.stream()
            .map(CouponMembershipPassResponse::from)
            .toList()
    );
  }

  @Schema(description = "쿠폰 이용권")
  public record CouponMembershipPassResponse(
      @Schema(description = "이용권 ID")
      String membershipPassId,

      @Schema(description = "쿠폰명", example = "2개월 이용권 쿠폰")
      String couponName,

      @Schema(description = "쿠폰 설명", example = "Retrivr 출시 이벤트")
      String description,

      @Schema(description = "이용 기간(일)", example = "60")
      int durationDays,

      @Schema(description = "이용권 상태", example = "사용전")
      String status,

      @Schema(description = "사용 시작 일시. 대기 중이면 활성화 예정 일시", example = "26.01.01")
      String startAt,

      @Schema(description = "사용 종료 예정 일시", example = "26.01.31")
      String endAt
  ) {

    public static CouponMembershipPassResponse from(MembershipPass membershipPass) {
      Coupon coupon = membershipPass.getCouponRegistrationOrThrow().getCoupon();
      String status = switch (membershipPass.getStatus()) {
        case ACTIVE -> "사용중";
        case REGISTERED -> "사용전";
        default -> "만료됨";
      };

      String startAt = membershipPass.getStartAt().toLocalDate().toString();
      startAt = startAt.replace("-", ".");
      startAt = startAt.substring(2);

      String endAt = membershipPass.getEndAt().toLocalDate().toString();
      endAt = endAt.replace("-", ".");
      endAt = endAt.substring(2);

      return new CouponMembershipPassResponse(
          membershipPass.getId(),
          coupon.getName(),
          coupon.getDescription(),
          coupon.getDurationDays(),
          status,
          startAt,
          endAt
      );
    }
  }
}
