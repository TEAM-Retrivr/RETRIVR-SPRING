package retrivr.retrivrspring.infrastructure.excel.sheet.factory;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.StatisticsLedgerRow;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelColumnSpec;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;
import retrivr.retrivrspring.infrastructure.excel.sheet.SummaryStatisticRow;

@Component
@Order(3)
public class SummaryStatisticsSheetFactory implements LedgerSheetFactory {

  @Override
  public ExcelSheetSpec<SummaryStatisticRow> create(LedgerExportRequest request) {
    StatisticsLedgerRow statistics = request.statisticsLedgerRow();

    List<SummaryStatisticRow> rows = List.of(
        SummaryStatisticRow.of("총 대여 횟수", statistics.totalRentalCount()),
        SummaryStatisticRow.of("현재 대여 건수", statistics.currentRentalCount()),
        SummaryStatisticRow.of("총 연체 건수", statistics.totalOverdueCount()),
        SummaryStatisticRow.of("연체 비율", formatPercent(statistics.overdueRate())),
        SummaryStatisticRow.of("평균 대여 기간", formatDays(statistics.averageRentalPeriod())),
        SummaryStatisticRow.of("가장 많이 대여된 물품", statistics.mostRentedItem())
    );

    return ExcelSheetSpec.of(
        "요약 통계",
        columns(),
        rows
    );
  }

  private List<ExcelColumnSpec<SummaryStatisticRow>> columns() {
    return List.of(
        ExcelColumnSpec.of("항목", SummaryStatisticRow::label),
        ExcelColumnSpec.of("값", SummaryStatisticRow::value)
    );
  }

  private String formatPercent(Number value) {
    if (value == null) {
      return "";
    }
    return value + "%";
  }

  private String formatDays(Number value) {
    if (value == null) {
      return "";
    }
    return value + "일";
  }
}
