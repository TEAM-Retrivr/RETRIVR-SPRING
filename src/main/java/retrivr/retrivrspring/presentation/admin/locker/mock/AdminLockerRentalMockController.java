package retrivr.retrivrspring.presentation.admin.locker.mock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.*;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockData;
import io.swagger.v3.oas.annotations.Parameter;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerStatus;

@RestController
@RequestMapping("/api/admin/v1/locker-rentals")
@Tag(name = "MOCK / 2.4 사물함 대여관리", description = LockerMockData.DESCRIPTION)
public class AdminLockerRentalMockController {

  @GetMapping("/overview")
  @Operation(summary = "[MOCK] 반납/관리 화면: 구역, 상태별 수량, 배치/목록 및 대여자 정보")
  public Overview overview(@RequestParam(required = false) UUID zoneId,
      @RequestParam(required = false) LockerStatus filter,
      @RequestParam(required = false) String keyword,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.overview(); }

  @GetMapping
  @Operation(summary = "[MOCK] 대여 목록/검색/이력")
  public Rentals rentals(@RequestParam(required = false) UUID zoneId,
      @RequestParam(required = false) LockerRentalStatus status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.rentals(); }

  @GetMapping("/{rentalId}")
  @Operation(summary = "[MOCK] 대여 상세/대여자 입력정보")
  public Rental detail(@PathVariable UUID rentalId,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.rental(); }

  @PostMapping("/{rentalId}/approve")
  @Operation(summary = "[MOCK] 대여 승인")
  public Action approve(@PathVariable UUID rentalId, @Valid @RequestBody ApprovalRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.rentalAction(LockerRentalStatus.APPROVED); }

  @PostMapping("/{rentalId}/reject")
  @Operation(summary = "[MOCK] 대여 거절")
  public Action reject(@PathVariable UUID rentalId, @Valid @RequestBody RejectRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.rentalAction(LockerRentalStatus.REJECTED); }

  @PostMapping("/{rentalId}/return")
  @Operation(summary = "[MOCK] 반납 처리")
  public Action returnRental(@PathVariable UUID rentalId, @Valid @RequestBody ManagerRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.rentalAction(LockerRentalStatus.RETURNED); }

  @PostMapping("/approve")
  @Operation(summary = "[MOCK] 선택한 대여 일괄 승인")
  public BatchAction approveBatch(@Valid @RequestBody BatchApprovalRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.batch(LockerRentalStatus.APPROVED); }

  @PostMapping("/return")
  @Operation(summary = "[MOCK] 선택한 대여 일괄 반납")
  public BatchAction returnBatch(@Valid @RequestBody BatchRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.batch(LockerRentalStatus.RETURNED); }
}
