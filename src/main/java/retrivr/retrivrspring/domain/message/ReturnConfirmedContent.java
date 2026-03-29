package retrivr.retrivrspring.domain.message;

public class ReturnConfirmedContent extends MessageContent {

  private final String organizationName;
  private final String itemName;

  public ReturnConfirmedContent(String organizationName, String itemName) {
    this.organizationName = organizationName;
    this.itemName = itemName;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 반납이 확인되었습니다";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "대여지명: %s%n대여 물품명: %s%n반납이 정상적으로 확인되었습니다.%n이용해주셔서 감사합니다.",
        organizationName,
        itemName
    );
  }
}
