package retrivr.retrivrspring.infrastructure.excel.data;

import java.util.List;

public record ExcelSheetSpec<T>(
    String sheetName,
    List<ExcelColumnSpec<T>> columns,
    List<T> rows
) {
  public static <T> ExcelSheetSpec<T> of(
      String sheetName,
      List<ExcelColumnSpec<T>> columns,
      List<T> rows
  ) {
    return new ExcelSheetSpec<>(sheetName, columns, rows);
  }
}
