package retrivr.retrivrspring.domain.message;

public class RequestCompletedContent extends MessageContent {

  private final String organizationName;
  private final String itemName;

  public RequestCompletedContent(String organizationName, String itemName) {
    this.organizationName = organizationName;
    this.itemName = itemName;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 대여 요청이 접수되었습니다";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "%s에 %s 대여 요청이 정상적으로 접수되었습니다.\n승인 결과는 등록한 이메일로 다시 안내드리겠습니다.",
        organizationName,
        itemName
    );
  }
}
