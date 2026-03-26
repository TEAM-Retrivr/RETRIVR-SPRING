package retrivr.retrivrspring.infrastructure.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.OutboundMessage;

@Slf4j
@Component
public class MockMessageSender implements MessageSender {

  @Override
  public void send(OutboundMessage message) throws Exception {
    log.info("[MOCK MESSAGE] phone={}, content={}",
        message.phone(),
        message.content().getMessage()
    );
  }
}
