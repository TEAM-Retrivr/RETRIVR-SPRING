package retrivr.retrivrspring.domain.message;

public class RentalRejectedContent extends MessageContent {

  private final String organizationName;
  private final String itemName;

  public RentalRejectedContent(String organizationName, String itemName) {
    this.organizationName = organizationName;
    this.itemName = itemName;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 대여 요청이 거절되었습니다";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "대여지명: %s%n대여 물품명: %s%n대여 요청이 거절되었습니다.%n자세한 내용은 대여 기관에 문의해 주세요.",
        organizationName,
        itemName
    );
  }
}
