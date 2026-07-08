package retrivr.retrivrspring.presentation.admin.membership.paymentmethod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.membership.pay.PaymentMethodService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.req.PaymentMethodCreateRequest;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res.PaymentMethodDeleteResponse;
import retrivr.retrivrspring.presentation.admin.membership.paymentmethod.res.PaymentMethodResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/payment-methods")
@Tag(name = "Admin API / Payment Method API", description = "조직 결제수단 관리")
public class PaymentMethodController {

  private final PaymentMethodService paymentMethodService;

  @PostMapping
  @Operation(
      summary = "결제수단 추가",
      description = """
          현재 조직에 PortOne V2 billingKey 기반 결제수단을 추가합니다.
          카드번호, CVC, 유효기간은 저장하지 않고 billingKey만 저장합니다.
          첫 번째 결제수단은 자동으로 기본 결제수단이 됩니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "결제수단 추가 성공",
      content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))
  )
  public PaymentMethodResponse createPaymentMethod(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @Valid @RequestBody PaymentMethodCreateRequest request
  ) {
    return paymentMethodService.createPaymentMethod(loginUser.organizationId(), request);
  }

  @GetMapping
  @Operation(summary = "결제수단 목록 조회")
  public List<PaymentMethodResponse> getPaymentMethods(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return paymentMethodService.getPaymentMethods(loginUser.organizationId());
  }

  @GetMapping("/{paymentMethodId}")
  @Operation(summary = "결제수단 단건 조회")
  public PaymentMethodResponse getPaymentMethod(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @PathVariable String paymentMethodId
  ) {
    return paymentMethodService.getPaymentMethod(loginUser.organizationId(), paymentMethodId);
  }

  @PatchMapping("/{paymentMethodId}/default")
  @Operation(
      summary = "기본 결제수단 변경",
      description = """
          현재 조직의 기본 결제수단을 변경합니다.
          기존 기본 결제수단은 기본 상태에서 해제되며, 구독이 있으면 해당 결제수단으로 연결합니다.
          """
  )
  public PaymentMethodResponse changeDefaultPaymentMethod(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @PathVariable String paymentMethodId
  ) {
    return paymentMethodService.changeDefaultPaymentMethod(
        loginUser.organizationId(),
        paymentMethodId
    );
  }

  @DeleteMapping("/{paymentMethodId}")
  @Operation(
      summary = "결제수단 삭제",
      description = """
          결제수단을 비활성화합니다.
          현재 활성 구독에서 사용 중인 결제수단은 삭제할 수 없습니다.
          """
  )
  public PaymentMethodDeleteResponse deletePaymentMethod(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
      @PathVariable String paymentMethodId
  ) {
    return paymentMethodService.deletePaymentMethod(loginUser.organizationId(), paymentMethodId);
  }
}
