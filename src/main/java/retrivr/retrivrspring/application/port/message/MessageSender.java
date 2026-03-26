package retrivr.retrivrspring.application.port.message;

public interface MessageSender {

  void send(OutboundMessage message) throws Exception;
}
