package retrivr.retrivrspring.domain.message;

public class OverdueReminderContent extends MessageContent {

  private final String organizationName;
  private final String itemName;
  private final Integer overdueDays;

  public OverdueReminderContent(
      String organizationName,
      String itemName,
      Integer overdueDays
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
        "%s입니다.\n %s %s일 연체되었습니다. 빠른 시일 내에 반납부탁드립니다.",
        organizationName,
        itemName,
        overdueDays
    );
  }
}
