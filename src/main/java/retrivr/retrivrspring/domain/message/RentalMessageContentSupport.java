package retrivr.retrivrspring.domain.message;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

abstract class RentalMessageContentSupport extends MessageContent {

  private static final String FIELD_GAP = "      ";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final String itemName;
  private final String itemDetailName;
  private final String organizationName;

  protected RentalMessageContentSupport(
      String itemName,
      String itemDetailName,
      String organizationName
  ) {
    this.itemName = itemName;
    this.itemDetailName = itemDetailName;
    this.organizationName = organizationName;
  }

  @Override
  protected String buildTemplateTitle() {
    return itemName;
  }

  protected String itemDetailLine() {
    return templateFieldLine("물품 상세", itemDetailName);
  }

  protected String templateFieldLine(String label, String value) {
    return label + FIELD_GAP + value;
  }

  protected String organizationName() {
    return organizationName;
  }

  protected String itemName() {
    return itemName;
  }

  protected String formatDateTime(LocalDateTime value) {
    return value.format(DATE_TIME_FORMATTER);
  }

  protected String formatDate(LocalDate value) {
    return value.format(DATE_FORMATTER);
  }

  protected String formatDate(LocalDateTime value) {
    return value.toLocalDate().format(DATE_FORMATTER);
  }
}
