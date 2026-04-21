package retrivr.retrivrspring.infrastructure.excel.data;

import java.util.function.Function;

public record ExcelColumnSpec<T>(
    String header,
    Function<T, Object> valueExtractor
) {
  public static <T> ExcelColumnSpec<T> of(String header, Function<T, Object> valueExtractor) {
    return new ExcelColumnSpec<>(header, valueExtractor);
  }
}
