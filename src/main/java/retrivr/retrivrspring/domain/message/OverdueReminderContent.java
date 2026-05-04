package retrivr.retrivrspring.domain.message;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OverdueReminderContent extends RentalMessageContentSupport {

  private final LocalDateTime decidedAt;
  private final LocalDate dueDate;
  private final int rentalDuration;

  public OverdueReminderContent(
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
    return "[Retrivr] " + itemName() + " 연체 안내";
  }

  @Override
  protected String buildBody() {
    return String.format(
        "%s입니다.%n대여한 %s의 반납기한이 지났습니다.%n빠른 시일 내에 반납 부탁드립니다.",
        organizationName(),
        itemName()
    );
  }

  @Override
  protected String buildTemplateCode() {
    return "rtr-v1-rental-overdue";
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        itemDetailLine(),
        templateFieldLine("대여 기간", rentalDuration + "일"),
        templateFieldLine("대여 일시", formatDateTime(decidedAt)),
        templateFieldLine("반납 기한", formatDate(dueDate)),
        "",
        "[연체 안내]",
        organizationName() + "에서 빌려가신 물품의 반납 기한이 지났어요.",
        "빠른 반납 부탁드릴게요.",
        "※ 미반납 시 추가 안내가 진행될 수 있어요."
    );
  }
}
