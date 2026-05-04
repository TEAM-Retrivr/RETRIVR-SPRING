package retrivr.retrivrspring.infrastructure.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BizMsgSendRequest(
    @JsonProperty("message_type")
    String messageType,
    String phn,
    String profile,
    String tmplId,
    String title,
    String msg,
    String reserveDt
) {
}
