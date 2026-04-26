package retrivr.retrivrspring.infrastructure.excel.sheet;

public record SummaryStatisticRow(
    String label,
    Object value
) {
  public static SummaryStatisticRow of(String label, Object value) {
    return new SummaryStatisticRow(label, value);
  }
}
