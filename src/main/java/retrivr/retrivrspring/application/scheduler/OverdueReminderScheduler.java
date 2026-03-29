package retrivr.retrivrspring.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.message.SendMessageService;

@Component
@RequiredArgsConstructor
public class OverdueReminderScheduler {

  private final SendMessageService sendMessageService;

  @Scheduled(cron = "0 0 9 * * *")
  public void sendOverdueReminders() {
    sendMessageService.sendAllOverdueReminders();
  }
}
