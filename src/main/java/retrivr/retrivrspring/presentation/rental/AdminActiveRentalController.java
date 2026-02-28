package retrivr.retrivrspring.presentation.rental;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.rental.AdminActiveRentalService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalItemPageResponse;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalReturnRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalReturnResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminOverdueRentalItemPageResponse;
import retrivr.retrivrspring.presentation.rental.req.RentalItemUpdateDueDateRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDueDateUpdateResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminReturnItemUnitListPageResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "Admin API / Active Rental", description = "대여 현황 및 반납 관리")
public class AdminActiveRentalController {

  private final AdminActiveRentalService adminActiveRentalService;

  @GetMapping("/rentals/overdue")
  @Operation(summary = "연체된 물품 리스트 조회")
  @ApiResponse(
      responseCode = "200",
      description = "연체된 물품 리스트 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminOverdueRentalItemPageResponse.class))
  )
  public AdminOverdueRentalItemPageResponse getOverdueItemList(
      @Parameter(description = "커서(마지막 조회된 rentalId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminActiveRentalService.getOverdueItemList(loginUser.organizationId(), cursor, size);
  }

  @GetMapping("/items/rental-summary")
  @Operation(summary = "반납 화면에서의 물품 리스트 조회")
  @ApiResponse(
      responseCode = "200",
      description = "물품 리스트 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalItemPageResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
  public AdminRentalItemPageResponse getRentalItemSummaryList(
      @Parameter(description = "커서(마지막 조회된 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminActiveRentalService.getRentalItemSummaryList(loginUser.organizationId(), cursor, size);
  }

  @GetMapping("/items/{itemId}/rentals/active")
  @Operation(summary = "대여 중인 물품 상세 조회")
  @ApiResponse(
      responseCode = "200",
      description = "반납 상세 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminReturnItemUnitListPageResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ITEM, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION})
  public AdminReturnItemUnitListPageResponse getReturnDetail(
      @PathVariable("itemId") Long itemId,
      @Parameter(description = "커서(마지막 조회된 itemUnitId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminActiveRentalService.getReturnDetail(loginUser.organizationId(), itemId, cursor, size);
  }

  @PostMapping("/rentals/{rentalId}/return")
  @Operation(summary = "반납 확인")
  @ApiResponse(
      responseCode = "200",
      description = "반납 확인 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalReturnResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION, ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION, ErrorCode.AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION})
  public AdminRentalReturnResponse confirmReturn(
      @PathVariable("rentalId") Long rentalId,
      @RequestBody AdminRentalReturnRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminActiveRentalService.confirmReturn(loginUser.organizationId(), rentalId, request);
  }

  @PatchMapping("/rentals/{rentalId}/due-date")
  @Operation(summary = "반납 일자 수정")
  @ApiResponse(
      responseCode = "200",
      description = "반납 예정일 수정 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDueDateUpdateResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL, ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION, ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION})
  public AdminRentalDueDateUpdateResponse updateDueDate(
      @PathVariable("rentalId") Long rentalId,
      @RequestBody RentalItemUpdateDueDateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    return adminActiveRentalService.updateDueDate(loginUser.organizationId(), rentalId, request);
  }
}
