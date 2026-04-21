package retrivr.retrivrspring.application.port.excel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record LedgerExportRequest(
    List<RentalLedgerRow> rentalLedgerRows,
    List<InventoryLedgerRow> inventoryLedgerRows,
    StatisticsLedgerRow statisticsLedgerRow
) {

  public record RentalLedgerRow(
      String publicId,
      String itemName,
      String itemLabel,
      String borrowerName,
      String borrowerContact,
      String borrowerAdditionalInfo,
      String status,
      LocalDateTime requestedAt,
      LocalDateTime decidedAt,
      String decidedBy,
      LocalDate dueDate,
      LocalDateTime returnedAt,
      String receivedBy,
      Boolean isOverdue
  ) {

    public static RentalLedgerRow of(Rental rental) {
      return new RentalLedgerRow(
          rental.getPublicId(),
          rental.getItem().getName(),
          rental.getItemUnit() != null ? rental.getItemUnit().getLabel() : null,
          rental.getBorrower().getName(),
          rental.getBorrower().getPhoneNumber(),
          rental.getBorrower().getAllAdditionalInfo(),
          rental.getStatus().getKorean(),
          rental.getRequestedAt(),
          rental.getDecidedAt(),
          rental.getDecidedBy(),
          rental.getDueDate(),
          rental.getReturnedAt(),
          rental.getReceivedBy(),
          rental.isOverdue()
      );
    }
  }

  public record InventoryLedgerRow(
      String publicId,
      String name,
      int availableQuantity,
      int totalQuantity,
      int rentedQuantity,
      long rentalCount,
      int rentalDuration,
      boolean isActive
  ) {
    public static InventoryLedgerRow of(Item item, long rentalCount) {
      return new InventoryLedgerRow(
          item.getPublicId(),
          item.getName(),
          item.getAvailableQuantity(),
          item.getTotalQuantity(),
          item.getRentedQuantity(),
          rentalCount,
          item.getRentalDuration(),
          item.isActive()
      );
    }
  }

  public record StatisticsLedgerRow(
      long totalRentalCount,
      long currentRentalCount,
      long totalOverdueCount,
      float overdueRate, // %
      float averageRentalPeriod, // 일
      String mostRentedItem
  ) {
    public static StatisticsLedgerRow of(long totalRentalCount, long currentRentalCount, long totalOverdueCount, float overdueRate, float averageRentalPeriod, String mostRentedItem) {
      return new StatisticsLedgerRow(
          totalRentalCount,
          currentRentalCount,
          totalOverdueCount,
          overdueRate,
          averageRentalPeriod,
          mostRentedItem
      );
    }
  }
}
