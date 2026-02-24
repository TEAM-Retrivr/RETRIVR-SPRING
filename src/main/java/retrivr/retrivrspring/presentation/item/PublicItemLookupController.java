package retrivr.retrivrspring.presentation.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.item.ItemLookupService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExample;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.item.res.PublicItemListPageResponse;
import retrivr.retrivrspring.presentation.item.res.PublicItemDetailResponse;

@RequiredArgsConstructor
@RestController
@Tag(name = "Public API / Item", description = "대여자용 물품 조회/대여 요청")
@RequestMapping("/api/public/v1")
public class PublicItemLookupController {

  private final ItemLookupService itemLookupService;

  @GetMapping("/organizations/{organizationId}/items")
  @Operation(summary = "대여지(단체) 물품 목록 조회")
  @ApiResponse(
      responseCode = "200",
      description = "물품 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = PublicItemListPageResponse.class))
  )
  @ApiErrorCodeExample(ErrorCode.NOT_FOUND_ORGANIZATION)
  public PublicItemListPageResponse getOrgItems(
      @PathVariable Long organizationId,
      @Parameter(description = "커서(마지막 조회된 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return itemLookupService.publicOrganizationItemListLookup(organizationId, cursor, size);
  }

  @GetMapping("/items/{itemId}")
  @Operation(summary = "대여지(단체) 물품 상세 조회")
  @ApiResponse(
      responseCode = "200",
      description = "물품 상세 조회 성공",
      content = @Content(schema = @Schema(implementation = PublicItemDetailResponse.class))
  )
  @ApiErrorCodeExample(ErrorCode.NOT_FOUND_ITEM)
  public PublicItemDetailResponse getOrgItemDetail(@PathVariable Long itemId) {
    return itemLookupService.publicOrganizationItemLookup(itemId);
  }
}
