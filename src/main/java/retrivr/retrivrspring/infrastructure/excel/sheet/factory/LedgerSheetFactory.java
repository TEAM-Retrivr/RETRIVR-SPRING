package retrivr.retrivrspring.infrastructure.excel.sheet.factory;

import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;

public interface LedgerSheetFactory {
  ExcelSheetSpec<?> create(LedgerExportRequest request);
}
