package retrivr.retrivrspring.domain.message;

import java.time.LocalDateTime;

public class ReturnConfirmedContent extends RentalMessageContentSupport {

  private final LocalDateTime decidedAt;
  private final LocalDateTime returnedAt;

  public ReturnConfirmedContent(
      String organizationName,
      String itemName,
      String itemDetailName,
      LocalDateTime decidedAt,
      LocalDateTime returnedAt
  ) {
    super(itemName, itemDetailName, organizationName);
    this.decidedAt = decidedAt;
    this.returnedAt = returnedAt;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 반납이 완료됐어요.";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "단체명 %s%n대여 물품명 %s%n반납이 정상적으로 완료됐습니다.%n이용해주셔서 감사합니다.",
        organizationName(),
        itemName()
    );
  }

  @Override
  protected String buildTemplateCode() {
    return "rtr-v1-rental-returned";
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        itemDetailLine(),
        templateFieldLine("대여 일시", formatDateTime(decidedAt)),
        templateFieldLine("반납 일자", formatDate(returnedAt)),
        "",
        "[반납 완료]",
        "반납이 정상적으로 완료됐어요.",
        "이용해주셔서 감사합니다."
    );
  }
}
