package retrivr.retrivrspring.domain.message;

import java.time.LocalDateTime;

public class RentalRejectedContent extends RentalMessageContentSupport {

  private final LocalDateTime requestedAt;

  public RentalRejectedContent(
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
    return "[Retrivr] 대여 요청이 거절됐어요.";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "단체명 %s%n대여 물품명 %s%n대여 요청이 거절됐습니다.%n자세한 내용은 대여 기관에 문의해 주세요.",
        organizationName(),
        itemName()
    );
  }

  @Override
  protected String buildTemplateCode() {
    return "rtr-v1-rental-request-rejected";
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        templateFieldLine("요청 일시", formatDateTime(requestedAt)),
        "",
        "[대여 요청 거절]",
        organizationName() + "에서 물품 대여 요청이 거절됐어요.",
        "※ 일정 시간이 지나면 요청이 자동으로 취소될 수 있어요. 필요 시 다시 요청해주세요."
    );
  }
}
