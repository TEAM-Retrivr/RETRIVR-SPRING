package retrivr.retrivrspring.presentation.open.locker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockContract.*;
import retrivr.retrivrspring.presentation.locker.mock.LockerMockData;

@RestController
@RequestMapping("/api/public/v1")
@SecurityRequirements
@Tag(name = "MOCK / 2.1 사물함 대여자", description = LockerMockData.DESCRIPTION)
public class PublicLockerMockController {

  @GetMapping("/organizations/{organizationId}/locker-zones")
  @Operation(summary = "[MOCK] 사물함 구역 목록")
  public Zones zones(@PathVariable Long organizationId) { return LockerMockData.zones(); }

  @GetMapping("/locker-zones/{zoneId}/lockers")
  @Operation(summary = "[MOCK] 구역 사물함 배치/목록 (개인정보 제외)")
  public Layout layout(@PathVariable UUID zoneId,
      @RequestParam(required = false) Boolean availableOnly) { return LockerMockData.layout(); }

  @GetMapping("/lockers/{lockerId}/rental-form")
  @Operation(summary = "[MOCK] 대여 신청 화면: 약관/입력 항목/예상 대여 기간")
  public RentalForm form(@PathVariable UUID lockerId) { return LockerMockData.form(); }

  @PostMapping("/lockers/{lockerId}/rentals")
  @Operation(summary = "[MOCK] 사물함 대여 신청")
  public Created create(@PathVariable UUID lockerId,
      @Valid @RequestBody RentalCreateRequest request) { return LockerMockData.created(); }

  @GetMapping("/locker-rentals/{publicId}")
  @Operation(summary = "[MOCK] 공개 ID로 대여 요청 결과 조회")
  public Created status(@PathVariable String publicId) { return LockerMockData.created(); }

  @PostMapping("/locker-rentals/{publicId}/approve")
  @Operation(summary = "[MOCK] 현장 대여 승인 (관리자 코드 토큰 예시)")
  public Action approve(@PathVariable String publicId,
      @Valid @RequestBody ImmediateApprovalRequest request) { return LockerMockData.rentalAction("RENTED"); }

  @GetMapping("/lockers/{lockerId}/extension-form")
  @Operation(summary = "[MOCK] 연장 신청 화면: 기존/연장 반납일 및 입력 항목")
  public ExtensionForm extensionForm(@PathVariable UUID lockerId) { return LockerMockData.extensionForm(); }

  @PostMapping("/lockers/{lockerId}/extensions")
  @Operation(summary = "[MOCK] 사물함 연장 신청")
  public ExtensionCreated createExtension(@PathVariable UUID lockerId,
      @Valid @RequestBody ExtensionCreateRequest request) { return LockerMockData.extensionCreated(); }

  @GetMapping("/locker-rentals/{publicId}/extensions/latest")
  @Operation(summary = "[MOCK] 연장 요청 상태 조회")
  public ExtensionCreated extensionStatus(@PathVariable String publicId) { return LockerMockData.extensionCreated(); }

  @PostMapping("/locker-rentals/{publicId}/extensions/latest/approve")
  @Operation(summary = "[MOCK] 현장 연장 승인 (관리자 코드 토큰 예시)")
  public Action approveExtension(@PathVariable String publicId,
      @Valid @RequestBody ImmediateApprovalRequest request) { return LockerMockData.extensionAction("APPROVED"); }
}
