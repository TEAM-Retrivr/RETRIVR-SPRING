package retrivr.retrivrspring.infrastructure.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.application.port.excel.LedgerExporter;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelColumnSpec;
import retrivr.retrivrspring.infrastructure.excel.data.ExcelSheetSpec;

@Component
@RequiredArgsConstructor
public class ExcelLedgerExporter implements LedgerExporter {

  private final ExcelCellStyler styler;
  private final LedgerExcelSheetSpecFactory sheetSpecFactory;

  @Override
  public byte[] export(LedgerExportRequest request) {
    // 엑셀 시트, 컬럼 정의 생성
    List<ExcelSheetSpec<?>> sheets = sheetSpecFactory.createSheets(request);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      CellStyle headerStyle = styler.createHeaderStyle(workbook);
      CellStyle dateStyle = styler.createDateStyle(workbook, "yyyy-mm-dd");
      CellStyle dateTimeStyle = styler.createDateStyle(workbook, "yyyy-mm-dd hh:mm:ss");

      for (ExcelSheetSpec<?> sheet : sheets) {
        writeSheet(workbook, sheet, headerStyle, dateStyle, dateTimeStyle);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("엑셀 파일 생성에 실패했습니다.", e);
    }
  }

  private <T> void writeSheet(
      Workbook workbook,
      ExcelSheetSpec<T> sheetSpec,
      CellStyle headerStyle,
      CellStyle dateStyle,
      CellStyle dateTimeStyle
  ) {
    Sheet sheet = workbook.createSheet(sheetSpec.sheetName());

    writeHeader(sheet, sheetSpec.columns(), headerStyle);
    writeRows(sheet, sheetSpec.rows(), sheetSpec.columns(), dateStyle, dateTimeStyle);
    autoSizeColumns(sheet, sheetSpec.columns().size());
  }

  private <T> void writeHeader(Sheet sheet, List<ExcelColumnSpec<T>> columns, CellStyle headerStyle) {
    Row headerRow = sheet.createRow(0);

    for (int i = 0; i < columns.size(); i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(columns.get(i).header());
      cell.setCellStyle(headerStyle);
    }
  }

  private <T> void writeRows(
      Sheet sheet,
      List<T> rows,
      List<ExcelColumnSpec<T>> columns,
      CellStyle dateStyle,
      CellStyle dateTimeStyle
  ) {
    int rowIndex = 1;

    for (T rowData : rows) {
      Row row = sheet.createRow(rowIndex++);

      for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
        ExcelColumnSpec<T> column = columns.get(colIndex);
        Object value = column.valueExtractor().apply(rowData);

        Cell cell = row.createCell(colIndex);
        writeCellValue(cell, value, dateStyle, dateTimeStyle);
      }
    }
  }

  private void writeCellValue(
      Cell cell,
      Object value,
      CellStyle dateStyle,
      CellStyle dateTimeStyle
  ) {
    if (value == null) {
      cell.setCellValue("");
      return;
    }

    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
      return;
    }

    if (value instanceof Boolean bool) {
      cell.setCellValue(bool);
      return;
    }

    if (value instanceof LocalDate localDate) {
      cell.setCellValue(Date.from(localDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()));
      cell.setCellStyle(dateStyle);
      return;
    }

    if (value instanceof LocalDateTime localDateTime) {
      cell.setCellValue(Date.from(localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()));
      cell.setCellStyle(dateTimeStyle);
      return;
    }

    cell.setCellValue(String.valueOf(value));
  }

  private void autoSizeColumns(Sheet sheet, int columnSize) {
    for (int i = 0; i < columnSize; i++) {
      sheet.autoSizeColumn(i);
      int currentWidth = sheet.getColumnWidth(i);
      sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 255 * 256));
    }
  }
}
