package retrivr.retrivrspring.infrastructure.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BizMsgResponseWrapper(
    String code,
    String message,
    JsonNode data,
    JsonNode error
) {
}
