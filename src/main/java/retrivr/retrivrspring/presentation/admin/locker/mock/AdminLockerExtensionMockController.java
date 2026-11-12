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
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerExtensionStatus;

@RestController
@RequestMapping("/api/admin/v1/locker-rental-extensions")
@Tag(name = "MOCK / 2.5 사물함 대여 연장 관리", description = LockerMockData.DESCRIPTION)
public class AdminLockerExtensionMockController {

  @GetMapping
  @Operation(summary = "[MOCK] 연장 요청 목록/검색/이력")
  public Extensions extensions(@RequestParam(required = false) UUID zoneId,
      @RequestParam(required = false) LockerExtensionStatus status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.extensions(); }

  @GetMapping("/{extensionId}")
  @Operation(summary = "[MOCK] 연장 요청 상세")
  public Extension detail(@PathVariable UUID extensionId,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.extension(); }

  @PostMapping("/{extensionId}/approve")
  @Operation(summary = "[MOCK] 연장 승인")
  public Action approve(@PathVariable UUID extensionId, @Valid @RequestBody ManagerRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.extensionAction("APPROVED"); }

  @PostMapping("/{extensionId}/reject")
  @Operation(summary = "[MOCK] 연장 거절")
  public Action reject(@PathVariable UUID extensionId, @Valid @RequestBody RejectRequest request,
      @Parameter(hidden = true) @AuthOrg AuthUser authUser) { return LockerMockData.extensionAction("REJECTED"); }
}
