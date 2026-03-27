package retrivr.retrivrspring.application.port.message;

public interface MessageSender {

  NotificationChannel channel();

  void send(NotificationRequest request) throws Exception;
}
