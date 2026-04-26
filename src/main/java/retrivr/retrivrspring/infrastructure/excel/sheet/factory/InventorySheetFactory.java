package retrivr.retrivrspring.infrastructure.excel.sheet.factory;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.InventoryLedgerRow;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelColumnSpec;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;

@Component
@Order(2)
public class InventorySheetFactory implements LedgerSheetFactory {

  @Override
  public ExcelSheetSpec<InventoryLedgerRow> create(LedgerExportRequest request) {
    return ExcelSheetSpec.of(
        "물품 현황",
        columns(),
        request.inventoryLedgerRows()
    );
  }

  private List<ExcelColumnSpec<InventoryLedgerRow>> columns() {
    return List.of(
        ExcelColumnSpec.of("ID", InventoryLedgerRow::publicId),
        ExcelColumnSpec.of("물품명", InventoryLedgerRow::name),
        ExcelColumnSpec.of("총 수량", InventoryLedgerRow::totalQuantity),
        ExcelColumnSpec.of("대여 가능 수량", InventoryLedgerRow::availableQuantity),
        ExcelColumnSpec.of("대여 중인 수량", InventoryLedgerRow::rentedQuantity),
        ExcelColumnSpec.of("대여 횟수", InventoryLedgerRow::rentalCount),
        ExcelColumnSpec.of("대여 기간", InventoryLedgerRow::rentalDuration),
        ExcelColumnSpec.of("활성 여부", row -> row.isActive() ? "활성" : "비활성")
    );
  }
}
