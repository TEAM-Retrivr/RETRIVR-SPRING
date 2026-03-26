package retrivr.retrivrspring.domain.message;

public class SendAllOverdueReminderPolicy {

  public static boolean shouldSend(long overdueDays) {
    return overdueDays == 1 || overdueDays == 3 || overdueDays % 7 == 0;
  }
}
