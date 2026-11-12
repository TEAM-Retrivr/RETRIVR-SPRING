package retrivr.retrivrspring.presentation.locker.mock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerExtensionStatus;

/** RTR-382: 프론트 연동용 임시 계약. 엔티티나 DB를 변경하지 않는다. */
public final class LockerMockContract {
  private LockerMockContract() {}

  @Schema(name = "LockerMockZone")
  public record Zone(UUID zoneId, String name, int rows, int columns,
                     int totalCount, int availableCount) {}
  @Schema(name = "LockerMockZones")
  public record Zones(List<Zone> zones) {}
  @Schema(name = "LockerMockBorrowerField")
  public record BorrowerField(String fieldKey, String label, boolean required, int sortOrder) {}
  @Schema(name = "LockerMockPolicy")
  public record Policy(int rentalDays, int maxExtensionCount, int extensionDays,
                       String terms, long termsVersion, String collateral,
                       List<BorrowerField> borrowerFields) {}
  @Schema(name = "LockerMockPublicLocker", description = "공개 배치/목록 공용. 대여자 개인정보는 제공하지 않습니다.")
  public record PublicLocker(UUID lockerId, String label, int rowIndex, int columnIndex,
                             UUID parentId, LockerStatus status, boolean extensionAvailable) {}
  @Schema(name = "LockerMockLayout")
  public record Layout(Zone zone, List<PublicLocker> lockers) {}
  @Schema(name = "LockerMockRentalForm")
  public record RentalForm(String organizationName, Zone zone, PublicLocker locker,
                           Policy policy, LocalDate startDate, LocalDate dueDate) {}
  @Schema(name = "LockerMockBorrower")
  public record Borrower(String name, String phone, String email,
                         Map<String, String> additionalInfo, String requestNote) {}
  @Schema(name = "LockerMockBorrowerRequest")
  public record BorrowerRequest(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Pattern(regexp = "[0-9]{10,11}") String phone,
      @Email @Size(max = 255) String email,
      @NotNull Map<@NotBlank String, @NotBlank String> additionalInfo,
      @Size(max = 30) String requestNote,
      @Schema(description = "목에서는 값 존재 여부만 검사하며 SMS 발송/인증을 수행하지 않습니다.")
      @NotBlank String phoneVerificationToken) {}
  @Schema(name = "LockerMockRentalCreateRequest")
  public record RentalCreateRequest(
      @NotNull @Valid BorrowerRequest borrower,
      @NotNull @AssertTrue Boolean termsAgreed,
      @NotNull @Positive Long termsVersion) {}
  @Schema(name = "LockerMockExtensionCreateRequest")
  public record ExtensionCreateRequest(@NotNull @Valid BorrowerRequest borrower) {}
  @Schema(name = "LockerMockManagerRequest")
  public record ManagerRequest(@NotBlank @Size(max = 255) String managerName) {}
  @Schema(name = "LockerMockApprovalRequest")
  public record ApprovalRequest(@NotBlank @Size(max = 255) String managerName,
                                @NotNull @AssertTrue Boolean collateralConfirmed) {}
  @Schema(name = "LockerMockImmediateApprovalRequest")
  public record ImmediateApprovalRequest(
      @NotBlank @Size(max = 100) String adminNameToApprove,
      @Schema(description = "목 전용 예시 토큰. 실제 관리자 코드 인증은 수행하지 않습니다.")
      @NotBlank String adminCodeVerificationToken) {}
  @Schema(name = "LockerMockRejectRequest")
  public record RejectRequest(@NotBlank @Size(max = 255) String managerName,
                              @NotBlank @Size(max = 255) String rejectReason) {}
  @Schema(name = "LockerMockBatchRequest")
  public record BatchRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> rentalIds,
                             @NotBlank @Size(max = 255) String managerName) {}
  @Schema(name = "LockerMockBatchApprovalRequest")
  public record BatchApprovalRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> rentalIds,
                                     @NotBlank @Size(max = 255) String managerName,
                                     @NotNull @AssertTrue Boolean collateralConfirmed) {}
  @Schema(name = "LockerMockAvailabilityRequest")
  public record AvailabilityRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> lockerIds,
                                    @NotNull Boolean available) {}
  @Schema(name = "LockerMockAction")
  public record Action(UUID id, String status, LocalDateTime processedAt) {}
  @Schema(name = "LockerMockBatchAction")
  public record BatchAction(List<Action> results) {}
  @Schema(name = "LockerMockCreated")
  public record Created(UUID rentalId, String publicId, LockerRentalStatus status,
                        LocalDateTime requestedAt, LocalDateTime expiresAt) {}
  @Schema(name = "LockerMockExtensionCreated")
  public record ExtensionCreated(UUID extensionId, String rentalPublicId,
                                 LockerExtensionStatus status, LocalDateTime requestedAt,
                                 LocalDate previousDueDate, LocalDate newDueDate) {}
  @Schema(name = "LockerMockManagedLocker")
  public record ManagedLocker(PublicLocker locker, UUID rentalId, Borrower borrower,
                              LocalDate startDate, LocalDate dueDate, boolean overdue) {}
  @Schema(name = "LockerMockSummary")
  public record Summary(int totalCount, int pendingCount, int availableCount,
                        int rentedCount, int unavailableCount) {}
  @Schema(name = "LockerMockOverview")
  public record Overview(List<Zone> zones, Zone selectedZone, Summary summary,
                         List<ManagedLocker> lockers) {}
  @Schema(name = "LockerMockRental", description = "요청/대여/반납 이력. 연체는 상태와 별도입니다.")
  public record Rental(UUID rentalId, String publicId, UUID lockerId, UUID zoneId,
                       String zoneName, String lockerLabel, LockerRentalStatus status,
                       Borrower borrower, LocalDateTime requestedAt, LocalDateTime expiresAt,
                       LocalDate startDate, LocalDate dueDate, boolean overdue,
                       int extensionCount, LocalDateTime approvedAt, String approvedBy,
                       LocalDateTime rejectedAt, String rejectedBy, String rejectReason,
                       LocalDateTime returnedAt, String returnedBy) {}
  @Schema(name = "LockerMockRentals")
  public record Rentals(List<Rental> rentals, int page, int size, long totalElements,
                         int totalPages) {}
  @Schema(name = "LockerMockExtensionForm")
  public record ExtensionForm(UUID rentalId, UUID lockerId, String zoneName, String lockerLabel,
                              LocalDate startDate, LocalDate previousDueDate, LocalDate newDueDate,
                              int extensionCount, int maxExtensionCount,
                              List<BorrowerField> borrowerFields) {}
  @Schema(name = "LockerMockExtension")
  public record Extension(UUID extensionId, UUID rentalId, String rentalPublicId,
                          String zoneName, String lockerLabel, Borrower borrower,
                          LockerExtensionStatus status, LocalDateTime requestedAt,
                          LocalDate previousDueDate, LocalDate newDueDate, long sequence,
                          LocalDateTime approvedAt, String approvedBy,
                          LocalDateTime rejectedAt, String rejectedBy, String rejectReason) {}
  @Schema(name = "LockerMockExtensions")
  public record Extensions(List<Extension> extensions, int page, int size,
                            long totalElements, int totalPages) {}
}
