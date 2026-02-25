package retrivr.retrivrspring.presentation.rental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.rental.AdminRentalRequestService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalApproveRequest;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalRejectRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDecisionResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/v1/rentals")
@Tag(name = "Admin API / Rental Request", description = "대여 요청 관리")
public class AdminRentalRequestController {

  private final AdminRentalRequestService adminRentalRequestService;

  @GetMapping("/requests")
  @Operation(summary = "대여 요청 목록 조회(요청됨 상태)")
  @ApiResponse(
      responseCode = "200",
      description = "대여 요청 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalRequestPageResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
  public AdminRentalRequestPageResponse getRequestedList(
      @Parameter(description = "커서(마지막 조회된 rentalId). 다음 페이지 조회 시 사용", example = "1001")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size,
      //todo: 로그인 기능 완성 시 변경
      @RequestParam(name = "login") Long mockOrganizationId
  ) {

    return adminRentalRequestService.getRequestedList(mockOrganizationId, cursor, size);
  }

  @PostMapping("/{rentalId}/approve")
  @Operation(summary = "대여 요청 승인")
  @ApiResponse(
      responseCode = "200",
      description = "승인 처리 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDecisionResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION, ErrorCode.AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION, ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION})
  public AdminRentalDecisionResponse approve(
      @PathVariable Long rentalId,
      @RequestBody AdminRentalApproveRequest request,
      //todo: 로그인 기능 완성 시 변경
      @RequestParam(name = "login") Long mockOrganizationId
  ) {
    return adminRentalRequestService.approveRentalRequest(rentalId, request, mockOrganizationId);
  }

  @PostMapping("/{rentalId}/reject")
  @Operation(summary = "대여 요청 거부")
  @ApiResponse(
      responseCode = "200",
      description = "거부 처리 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDecisionResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION, ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION, ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION})
  public AdminRentalDecisionResponse reject(
      @PathVariable Long rentalId,
      @RequestBody AdminRentalRejectRequest request,
      @RequestParam(name = "login") Long mockOrganizationId
  ) {
    return adminRentalRequestService.rejectRentalRequest(rentalId, request, mockOrganizationId);
  }

}
