package retrivr.retrivrspring.presentation.rental;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.rental.PublicRentalService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalDetailResponse;

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
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ITEM, ErrorCode.NOT_FOUND_ITEM_UNIT, ErrorCode.ILLEGAL_BORROWER_FIELD})
  public PublicRentalCreateResponse createRental(
      @PathVariable("itemId") Long itemId,
      @RequestBody PublicRentalCreateRequest request
  ) {
    return publicRentalService.requestRental(itemId, request);
  }

  @GetMapping("/rentals/{rentalId}")
  @Operation(summary = "대여 상태/대여 정보 조회(승인 완료 확인용)")
  @ApiResponse(
      responseCode = "200",
      description = "대여 정보 조회 성공",
      content = @Content(schema = @Schema(implementation = PublicRentalDetailResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_RENTAL})
  public PublicRentalDetailResponse getRentalInfo(
      @PathVariable("rentalId") Long rentalId
  ) {
    return publicRentalService.checkRentalStatusAndDetail(rentalId);
  }

}
