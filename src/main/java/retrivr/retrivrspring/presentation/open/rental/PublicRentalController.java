package retrivr.retrivrspring.presentation.open.rental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.open.PublicRentalService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalImmediateApproveRequest;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalImmediateRejectRequest;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalDetailResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalImmediateApproveResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalImmediateRejectResponse;

@RequiredArgsConstructor
@RestController
@Tag(name = "Public Rental API", description = "대여자용 대여 요청/상태 조회")
@RequestMapping("/api/public/v1")
public class PublicRentalController {

  private final PublicRentalService publicRentalService;

  @PostMapping("/items/{itemId}/rentals")
  @Operation(summary = "대여 요청 생성(대여자 정보 입력 + 요청)")
  @ApiResponse(
      responseCode = "200",
      description = "대여 요청 생성 성공",
      content = @Content(schema = @Schema(implementation = PublicRentalCreateResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ITEM, ErrorCode.NOT_FOUND_ITEM_UNIT, ErrorCode.ILLEGAL_BORROWER_LABEL})
  public PublicRentalCreateResponse createRental(
      @PathVariable("itemId") Long itemId,
      @Valid @RequestBody PublicRentalCreateRequest request
  ) {
    return publicRentalService.requestRental(itemId, request);
  }

  @GetMapping("/rentals/{rentalId}")
  @Operation(summary = "대여 상태/대여 정보 조회(현장 즉시 완료용)")
  @ApiResponse(
      responseCode = "200",
      description = "대여 정보 조회 성공",
      content = @Content(schema = @Schema(implementation = PublicRentalDetailResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL})
  public PublicRentalDetailResponse getRentalInfo(
      @PathVariable("rentalId") Long rentalId,
      @RequestParam(name = "token") String token
  ) {
    return publicRentalService.checkRentalStatusAndDetail(rentalId, token);
  }

  @PostMapping("/rentals/{rentalId}/approve")
  @Operation(summary = "대여 요청 현장 즉시 승인")
  @ApiResponse(
      responseCode = "200",
      description = "승인 처리 성공",
      content = @Content(schema = @Schema(implementation = PublicRentalImmediateApproveResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, ErrorCode.AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION, ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION})
  public PublicRentalImmediateApproveResponse approve(
      @PathVariable Long rentalId,
      @Valid @RequestBody PublicRentalImmediateApproveRequest request
  ) {
    return publicRentalService.approveRentalRequest(rentalId, request);
  }

  @PostMapping("rentals/{rentalId}/reject")
  @Operation(summary = "대여 요청 현장 즉시 거부")
  @ApiResponse(
      responseCode = "200",
      description = "거부 처리 성공",
      content = @Content(schema = @Schema(implementation = PublicRentalImmediateRejectResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION, ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION, ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION})
  public PublicRentalImmediateRejectResponse reject(
      @PathVariable Long rentalId,
      @Valid @RequestBody PublicRentalImmediateRejectRequest request
  ) {
    return publicRentalService.rejectRentalRequest(rentalId, request);
  }
}
