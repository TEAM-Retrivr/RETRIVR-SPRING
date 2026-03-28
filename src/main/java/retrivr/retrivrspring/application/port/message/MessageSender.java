package retrivr.retrivrspring.application.port.message;

import retrivr.retrivrspring.global.error.ApplicationException;

public interface MessageSender {

  NotificationChannel channel();

  void send(NotificationRequest request) throws ApplicationException;
}
