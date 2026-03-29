package retrivr.retrivrspring.domain.message;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RentalApprovedContent extends MessageContent {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final String organizationName;
  private final String itemName;
  private final LocalDate dueDate;

  public RentalApprovedContent(String organizationName, String itemName, LocalDate dueDate) {
    this.organizationName = organizationName;
    this.itemName = itemName;
    this.dueDate = dueDate;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 대여 요청이 승인되었습니다";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "대여지명: %s%n대여 물품명: %s%n반납 예정일: %s%n대여 요청이 승인되었습니다.",
        organizationName,
        itemName,
        dueDate.format(DATE_FORMATTER)
    );
  }
}
