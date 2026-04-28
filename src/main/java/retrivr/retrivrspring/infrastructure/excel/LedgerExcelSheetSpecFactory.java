package retrivr.retrivrspring.infrastructure.excel;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;
import retrivr.retrivrspring.infrastructure.excel.sheet.factory.LedgerSheetFactory;

@Component
@RequiredArgsConstructor
public class LedgerExcelSheetSpecFactory {

  private final List<LedgerSheetFactory> sheetFactories;

  public List<ExcelSheetSpec<?>> createSheets(LedgerExportRequest request) {
    return sheetFactories.stream()
        .map(factory -> factory.create(request))
        .collect(Collectors.toList());
  }
}
