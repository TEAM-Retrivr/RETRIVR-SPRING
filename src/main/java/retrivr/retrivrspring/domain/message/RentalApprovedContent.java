package retrivr.retrivrspring.domain.message;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalApprovedContent extends RentalMessageContentSupport {

  private final LocalDateTime decidedAt;
  private final LocalDate dueDate;
  private final int rentalDuration;

  public RentalApprovedContent(
      String organizationName,
      String itemName,
      String itemDetailName,
      LocalDateTime decidedAt,
      LocalDate dueDate,
      int rentalDuration
  ) {
    super(itemName, itemDetailName, organizationName);
    this.decidedAt = decidedAt;
    this.dueDate = dueDate;
    this.rentalDuration = rentalDuration;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 대여가 승인됐어요.";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "단체명 %s%n대여 물품명 %s%n반납 예정일 %s%n대여 요청이 승인됐습니다.",
        organizationName(),
        itemName(),
        formatDate(dueDate)
    );
  }

  @Override
  protected String buildTemplateCode() {
    return "rtr-v1-rental-approved";
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        itemDetailLine(),
        templateFieldLine("대여 기간", rentalDuration + "일"),
        templateFieldLine("대여 일시", formatDateTime(decidedAt)),
        templateFieldLine("반납 기한", formatDate(dueDate)),
        "",
        "[대여 완료]",
        organizationName() + "에서 대여가 승인됐어요.",
        "반납 기한을 확인하고 기한 내 반납해주세요."
    );
  }
}
