package retrivr.retrivrspring.domain.message;

public class OverdueReminderContent extends MessageContent {

  private final String organizationName;
  private final String itemName;
  private final int overdueDays;

  public OverdueReminderContent(
      String organizationName,
      String itemName,
      int overdueDays
  ) {
    this.organizationName = organizationName;
    this.itemName = itemName;
    this.overdueDays = overdueDays;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] " + itemName + " 연체 안내";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "%s입니다.%n대여해 가신 %s이(가) %d일 연체되었습니다.%n빠른 시일 내에 반납 부탁드립니다.",
        organizationName,
        itemName,
        overdueDays
    );
  }
}
