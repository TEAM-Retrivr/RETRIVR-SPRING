package retrivr.retrivrspring.infrastructure.excel.sheet.factory;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.RentalLedgerRow;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelColumnSpec;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;

@Component
@Order(1)
public class RentalHistorySheetFactory implements LedgerSheetFactory {

  @Override
  public ExcelSheetSpec<RentalLedgerRow> create(LedgerExportRequest request) {
    return ExcelSheetSpec.of(
        "대여 이력",
        columns(),
        request.rentalLedgerRows()
    );
  }

  private List<ExcelColumnSpec<RentalLedgerRow>> columns() {
    return List.of(
        ExcelColumnSpec.of("ID", RentalLedgerRow::publicId),
        ExcelColumnSpec.of("물품명", RentalLedgerRow::itemName),
        ExcelColumnSpec.of("물품 라벨", RentalLedgerRow::itemLabel),
        ExcelColumnSpec.of("대여자 이름", RentalLedgerRow::borrowerName),
        ExcelColumnSpec.of("연락처", RentalLedgerRow::borrowerContact),
        ExcelColumnSpec.of("추가 정보", RentalLedgerRow::borrowerAdditionalInfo),
        ExcelColumnSpec.of("대여 상태", RentalLedgerRow::status),
        ExcelColumnSpec.of("대여 요청일시", RentalLedgerRow::requestedAt),
        ExcelColumnSpec.of("요청 결정일시", RentalLedgerRow::decidedAt),
        ExcelColumnSpec.of("요청 결정자", RentalLedgerRow::decidedBy),
        ExcelColumnSpec.of("반납 예정일", RentalLedgerRow::dueDate),
        ExcelColumnSpec.of("반납일시", RentalLedgerRow::returnedAt),
        ExcelColumnSpec.of("반납 승인자", RentalLedgerRow::receivedBy),
        ExcelColumnSpec.of("연체 여부", row -> row.isOverdue() ? "연체" : "정상")
    );
  }
}
