package retrivr.retrivrspring.presentation.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.internal.InternalCouponService;
import retrivr.retrivrspring.global.auth.manager.ValidManager;
import retrivr.retrivrspring.presentation.internal.req.InternalCouponCreateRequest;
import retrivr.retrivrspring.presentation.admin.coupon.res.CouponRegistrationResponse;
import retrivr.retrivrspring.presentation.internal.res.InternalCouponCreateResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/v1/coupons")
@Tag(name = "Internal API / Coupon API", description = "관리자/개발자용 쿠폰 API")
public class InternalCouponController {

  private final InternalCouponService internalCouponService;

  @PostMapping
  @Operation(
      summary = "쿠폰 생성",
      description = """
          새로운 쿠폰을 생성합니다.
          """
  )
  @ApiResponse(
      responseCode = "201",
      description = "쿠폰 생성 성공",
      content = @Content(schema = @Schema(implementation = CouponRegistrationResponse.class))
  )
  @ValidManager
  public InternalCouponCreateResponse createCoupon(
      @Valid @RequestBody InternalCouponCreateRequest request
  ) {
    return internalCouponService.createCoupon(request);
  }
}
