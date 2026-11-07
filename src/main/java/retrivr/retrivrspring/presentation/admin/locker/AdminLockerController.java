package retrivr.retrivrspring.presentation.admin.locker;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.application.service.admin.locker.AdminLockerCombinationService;
import retrivr.retrivrspring.application.service.admin.locker.AdminLockerService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerAvailabilityRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerCombinationRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerEnableRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerLabelUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerMutationResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerSearchPageResponse;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/lockers")
@Tag(name = "Admin API / Locker", description = "관리자 사물함 관리 API")
public class AdminLockerController {

  private final AdminLockerService service;
  private final AdminLockerCombinationService combinationService;

  @GetMapping("/search")
  @Operation(summary = "ADM-LOCKER-001 사물함 검색", description = "keyword로 라벨 또는 신청·대여 중인 대여자의 이름·전화번호·이메일 검색")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.BAD_REQUEST_EXCEPTION
  })
  public AdminLockerSearchPageResponse search(
      @Parameter(description = "검색 키워드" , example = "사물함 1")
      @RequestParam(name = "keyword")
      String keyword,
      @Parameter(description = "마지막으로 조회된 cursorLockerId", example = "uuid")
      @RequestParam(name = "cursorLocker", required = false)
      UUID cursorLockerId,
      @Parameter(description = "마지막으로 조회된 Score", example = "10.1")
      @RequestParam(name = "cursorScore", required = false)
      Double cursorScore,
      @RequestParam(name = "size", required = false, defaultValue = "15")
      Integer size,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return service.search(keyword, cursorLockerId, cursorScore, size, authUser.organizationId());
  }

  @PostMapping("/combination")
  @Operation(summary = "ADM-LOCKER-002 인접한 두 사물함 병합")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION,
      ErrorCode.LOCKER_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION,
      ErrorCode.LOCKER_OWNER_MISMATCH,
      ErrorCode.LOCKER_DISABLED,
      ErrorCode.LOCKER_NOT_REPRESENTATIVE,
      ErrorCode.LOCKER_SELF_COMBINATION_NOT_ALLOWED,
      ErrorCode.LOCKER_NOT_ADJACENT,
      ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS,
      ErrorCode.VERSION_CONFLICT
  })
  public AdminLockerMutationResponse combine(
      @Valid @RequestBody AdminLockerCombinationRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return combinationService.combination(authUser.organizationId(), request);
  }

  @PatchMapping("/{lockerId}/activation")
  @Operation(summary = "ADM-LOCKER-003 사물함 활성화 상태 변경")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.LOCKER_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION, ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS,
      ErrorCode.LOCKER_ALREADY_ENABLED, ErrorCode.LOCKER_ALREADY_DISABLED,
      ErrorCode.LOCKER_COMBINED_ENABLE_NOT_ALLOWED,
      ErrorCode.VERSION_CONFLICT
  })
  public AdminLockerMutationResponse updateActivation(
      @PathVariable UUID lockerId,
      @Valid @RequestBody AdminLockerEnableRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return service.updateEnable(authUser.organizationId(), lockerId, request);
  }

  @PatchMapping("/{lockerId}/label")
  @Operation(summary = "ADM-LOCKER-004 사물함 라벨 변경")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.LOCKER_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION, ErrorCode.INVALID_LOCKER_VALUE,
      ErrorCode.DUPLICATE_LOCKER_LABEL, ErrorCode.VERSION_CONFLICT
  })
  public AdminLockerMutationResponse updateLabel(
      @PathVariable UUID lockerId,
      @Valid @RequestBody AdminLockerLabelUpdateRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return service.updateLabel(authUser.organizationId(), lockerId, request);
  }

  @PatchMapping("/{lockerId}/availability")
  @Operation(summary = "ADM-LOCKER-005 사물함 대여 가능 여부 변경")
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION, ErrorCode.LOCKER_NOT_FOUND,
      ErrorCode.BAD_REQUEST_EXCEPTION, ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS,
      ErrorCode.LOCKER_DISABLED, ErrorCode.LOCKER_RENTAL_ENABLE_NOT_ALLOWED,
      ErrorCode.VERSION_CONFLICT
  })
  public AdminLockerMutationResponse updateAvailability(
      @PathVariable UUID lockerId,
      @Valid @RequestBody AdminLockerAvailabilityRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    return service.updateRentalAble(authUser.organizationId(), lockerId, request);
  }
}
