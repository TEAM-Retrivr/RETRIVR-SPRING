package retrivr.retrivrspring.infrastructure.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BizMsgSendResponse(
    String code,
    JsonNode data,
    String message,
    String originMessage
) {

  public boolean isSuccess() {
    return "success".equalsIgnoreCase(code);
  }
}
