package retrivr.retrivrspring.presentation.admin.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.item.AdminItemService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemPageResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUnitMutationResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUpdateResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/items")
@Tag(name = "Admin API / Item", description = "관리자 물품 관리 API")
public class AdminItemController {

  private final AdminItemService adminItemService;

  @GetMapping
  @Operation(summary = "관리자 물품 목록 조회")
  @ApiResponse(
      responseCode = "200",
      description = "물품 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminItemPageResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
  public AdminItemPageResponse getItems(
      @Parameter(description = "커서(마지막으로 조회한 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    Long organizationId = authUser.organizationId();
    return adminItemService.getItems(organizationId, cursor, size);
  }

  @PostMapping
  @Operation(summary = "관리자 물품 생성")
  @ApiResponse(
      responseCode = "200",
      description = "물품 생성 성공",
      content = @Content(schema = @Schema(implementation = AdminItemCreateResponse.class))
  )
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.BAD_REQUEST_EXCEPTION})
  public AdminItemCreateResponse createItem(
      @Valid @RequestBody AdminItemCreateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    Long organizationId = authUser.organizationId();
    return adminItemService.createItem(organizationId, request);
  }

  @PatchMapping("/{itemId}")
  @Operation(summary = "관리자 물품 수정")
  @ApiResponse(
      responseCode = "200",
      description = "물품 수정 성공",
      content = @Content(schema = @Schema(implementation = AdminItemUpdateResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.NOT_FOUND_ITEM,
      ErrorCode.BAD_REQUEST_EXCEPTION
  })
  public AdminItemUpdateResponse updateItem(
      @PathVariable Long itemId,
      @Valid @RequestBody AdminItemUpdateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    Long organizationId = authUser.organizationId();
    return adminItemService.updateItem(organizationId, itemId, request);
  }

  @PatchMapping("/{itemId}/units/{itemUnitId}/availability")
  @Operation(summary = "UNIT 물품 고유번호 대여 가능 여부 변경")
  @ApiResponse(
      responseCode = "200",
      description = "고유번호 대여 가능 여부 변경 성공",
      content = @Content(schema = @Schema(implementation = AdminItemUnitMutationResponse.class))
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.NOT_FOUND_ITEM,
      ErrorCode.NOT_FOUND_ITEM_UNIT,
      ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION,
      ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE,
      ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION
  })
  public AdminItemUnitMutationResponse updateUnitAvailability(
      @PathVariable Long itemId,
      @PathVariable Long itemUnitId,
      @Valid @RequestBody AdminItemUnitAvailabilityUpdateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    Long organizationId = authUser.organizationId();
    return adminItemService.updateUnitAvailability(organizationId, itemId, itemUnitId, request);
  }
}
