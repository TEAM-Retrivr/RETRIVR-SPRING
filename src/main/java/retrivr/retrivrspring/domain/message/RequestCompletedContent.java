package retrivr.retrivrspring.domain.message;

import java.time.LocalDateTime;

public class RequestCompletedContent extends RentalMessageContentSupport {

  private final LocalDateTime requestedAt;

  public RequestCompletedContent(
      String organizationName,
      String itemName,
      String itemDetailName,
      LocalDateTime requestedAt
  ) {
    super(itemName, itemDetailName, organizationName);
    this.requestedAt = requestedAt;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 대여 요청이 접수됐어요.";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "단체명 %s%n대여 물품명 %s%n대여 요청이 정상적으로 접수됐습니다.%n확인 결과는 등록된 이메일로 다시 안내해드리겠습니다.",
        organizationName(),
        itemName()
    );
  }

  @Override
  protected String buildTemplateCode() {
    return "rtr-v1-rental-request-complete";
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        itemDetailLine(),
        templateFieldLine("요청 일시", formatDateTime(requestedAt)),
        "",
        "[대여 요청 완료]",
        organizationName() + "에서 물품 대여 요청이 접수됐어요.",
        "관리자 승인 후 대여가 확정돼요.",
        "※ 대여 요청은 15분간 유지됩니다."
    );
  }
}
