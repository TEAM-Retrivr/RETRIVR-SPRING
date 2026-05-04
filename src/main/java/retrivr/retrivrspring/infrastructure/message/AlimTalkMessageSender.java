package retrivr.retrivrspring.infrastructure.message;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.NotificationChannel;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.error.InfraException;
import retrivr.retrivrspring.global.properties.AlimTalkProperties;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlimTalkMessageSender implements MessageSender {

  private static final String MESSAGE_TYPE = "AT";
  private static final String IMMEDIATE_SEND = "00000000000000";

  private final BizMsgClient bizMsgClient;
  private final AlimTalkProperties alimTalkProperties;

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.ALIM_TALK;
  }

  @Override
  public void send(NotificationRequest request) {
    String templateCode = requireValue(request.content().getTemplateCode(), "template code");
    String templateTitle = requireValue(request.content().getTemplateTitle(), "template title");
    String templateMessage = request.content().getTemplateMessage();

    BizMsgSendRequest payload = new BizMsgSendRequest(
        MESSAGE_TYPE,
        request.resolveRecipient(NotificationChannel.ALIM_TALK),
        alimTalkProperties.profileKey(),
        templateCode,
        templateTitle,
        templateMessage,
        IMMEDIATE_SEND
    );

    List<BizMsgSendResponse> responses = bizMsgClient.send(List.of(payload));
    BizMsgSendResponse response = responses.stream()
        .findFirst()
        .orElseThrow(() -> new InfraException(ErrorCode.BIZMSG_API_EMPTY_RESPONSE));

    if (!response.isSuccess()) {
      log.error("BizMsg send failed. tmplId={}, title={}, msg={}, response={}",
          templateCode, templateTitle, templateMessage, response);

      ErrorCode errorCode = isNoMatchedTemplate(response.message())
          ? ErrorCode.BIZMSG_TEMPLATE_INVALID
          : ErrorCode.BIZMSG_SEND_FAILED;
      throw new InfraException(
          errorCode,
          "code=%s, message=%s".formatted(response.code(), response.message())
      );
    }
  }

  private boolean isNoMatchedTemplate(String message) {
    return message != null && message.contains("K105:NoMatchedTemplate");
  }

  private String requireValue(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new InfraException(
          ErrorCode.BIZMSG_TEMPLATE_INVALID,
          "Missing " + fieldName + " for AlimTalk message."
      );
    }
    return value;
  }
}
