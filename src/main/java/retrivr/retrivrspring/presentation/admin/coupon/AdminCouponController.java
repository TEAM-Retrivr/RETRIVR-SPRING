package retrivr.retrivrspring.presentation.admin.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.coupon.CouponRegistrationService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.coupon.res.AdminCouponCodeCheckResponse;
import retrivr.retrivrspring.presentation.admin.coupon.res.CouponRegistrationResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/coupons")
@Tag(name = "Admin API / Coupon API", description = "조직의 쿠폰 CRUD API")
public class AdminCouponController {

  private final CouponRegistrationService couponRegistrationService;

  @PostMapping("/{couponId}/registrations")
  @Operation(
      summary = "쿠폰 등록",
      description = """
          쿠폰 코드를 등록하여 MembershipPass를 생성합니다.
          현재 사용 중인 이용권이 있으면 해당 이용권 뒤에 WAITING 상태로 이어붙입니다.
          사용 가능한 이용권이 없으면 즉시 ACTIVE 상태로 시작합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "쿠폰 등록 성공",
      content = @Content(schema = @Schema(implementation = CouponRegistrationResponse.class))
  )
  public CouponRegistrationResponse registerCoupon(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Valid @PathVariable("couponId") String couponId
  ) {
    return couponRegistrationService.registerCoupon(loginUser.organizationId(), couponId);
  }

  @GetMapping("/{couponCode}")
  @Operation(
      summary = "쿠폰 조회",
      description = """
          쿠폰 코드를 이용하여 쿠폰 정보를 조회합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "쿠폰 조회 성공",
      content = @Content(schema = @Schema(implementation = CouponRegistrationResponse.class))
  )
  public AdminCouponCodeCheckResponse checkCouponCode(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Valid @PathVariable("couponCode") String couponCode
  ) {
    return couponRegistrationService.checkCouponCode(loginUser.organizationId(), couponCode);
  }
}