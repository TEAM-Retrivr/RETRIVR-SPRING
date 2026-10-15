package retrivr.retrivrspring.presentation.admin.locker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.locker.AdminLockerZoneService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneCreateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneNameUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneOrderRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneLayoutUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.LockerFilteringStatus;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerLayoutResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneDeleteResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneListResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerSummaryResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/locker-zones")
@Tag(name = "Admin API / Locker Zone", description = "관리자 사물함 구역 관리 API")
public class AdminLockerZoneController {

  private final AdminLockerZoneService zoneService;

  @GetMapping
  @Operation(summary = "ADM-ZONE-001 사물함 구역 목록 조회")
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
  public AdminLockerZoneListResponse getZones(
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.getZones(authUser.organizationId());
  }

  @GetMapping("/{zoneId}/lockers")
  @Operation(summary = "ADM-ZONE-002 구역 사물함 조회")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.LOCKER_ZONE_NOT_FOUND
  })
  public AdminLockerLayoutResponse getLockers(
      @PathVariable UUID zoneId,
      @RequestParam(name = "filter", required = false) LockerFilteringStatus filter,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.getLockers(authUser.organizationId(), filter, zoneId);
  }


  @GetMapping("/{zoneId}/lockers/summary")
  @Operation(summary = "ADM-ZONE-003 구역 내 상태별 사물함 수량 조회")
  @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
  public AdminLockerSummaryResponse getSummary(
      @PathVariable UUID zoneId,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) {
    return zoneService.getLockersSummary(authUser.organizationId(), zoneId);
  }

  @PostMapping
  @Operation(summary = "ADM-ZONE-003 사물함 구역 및 초기 배치 생성")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.BAD_REQUEST_EXCEPTION,
      ErrorCode.INVALID_LOCKER_ZONE_VALUE,
      ErrorCode.DUPLICATE_LOCKER_ZONE_NAME
  })
  public AdminLockerZoneResponse createZone(
      @Valid @RequestBody AdminLockerZoneCreateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.createZone(authUser.organizationId(), request);
  }

  @PatchMapping("/{zoneId}/layout")
  @Operation(summary = "ADM-ZONE-004 구역 배치 크기 수정")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.LOCKER_ZONE_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION,
      ErrorCode.INVALID_LOCKER_ZONE_VALUE,
      ErrorCode.VERSION_CONFLICT,
      ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS
  })
  public AdminLockerZoneResponse updateZoneLayout(
      @PathVariable UUID zoneId,
      @Valid @RequestBody AdminLockerZoneLayoutUpdateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.updateZoneLayout(authUser.organizationId(), zoneId, request);
  }

  @PatchMapping("/{zoneId}/name")
  @Operation(summary = "ADM-ZONE-005 구역 이름 수정")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.LOCKER_ZONE_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION,
      ErrorCode.INVALID_LOCKER_ZONE_VALUE,
      ErrorCode.VERSION_CONFLICT,
      ErrorCode.DUPLICATE_LOCKER_ZONE_NAME
  })
  public AdminLockerZoneResponse updateZoneName(
      @PathVariable UUID zoneId,
      @Valid @RequestBody AdminLockerZoneNameUpdateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.updateZoneName(authUser.organizationId(), zoneId, request);
  }

  @PutMapping("/order")
  @Operation(summary = "ADM-ZONE-006 전체 구역 표시 순서 변경")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.BAD_REQUEST_EXCEPTION,
      ErrorCode.INVALID_LOCKER_ZONE_ORDER
  })
  public AdminLockerZoneListResponse reorderZones(
      @Valid @RequestBody AdminLockerZoneOrderRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.reorderZones(authUser.organizationId(), request);
  }

  @DeleteMapping("/{zoneId}")
  @Operation(summary = "ADM-ZONE-007 사물함 구역 삭제")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.LOCKER_ZONE_NOT_FOUND,
      ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS
  })
  public AdminLockerZoneDeleteResponse deleteZone(
      @PathVariable UUID zoneId,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return zoneService.deleteZone(authUser.organizationId(), zoneId);
  }
}
