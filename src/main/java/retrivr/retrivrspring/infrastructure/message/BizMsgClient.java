package retrivr.retrivrspring.infrastructure.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.error.InfraException;
import retrivr.retrivrspring.global.properties.AlimTalkProperties;

@Component
@Slf4j
@RequiredArgsConstructor
public class BizMsgClient {

  private static final TypeReference<List<BizMsgSendResponse>> RESPONSE_TYPE =
      new TypeReference<>() {};

  private final AlimTalkProperties alimTalkProperties;
  private final RestClient.Builder restClientBuilder;
  private final ObjectMapper objectMapper;

  public List<BizMsgSendResponse> send(List<BizMsgSendRequest> requests) {
    try {
      String responseBody = restClientBuilder
          .baseUrl(alimTalkProperties.host())
          .build()
          .post()
          .uri("/v2/sender/send")
          .contentType(MediaType.APPLICATION_JSON)
          .header("userid", alimTalkProperties.userId())
          .body(requests)
          .retrieve()
          .body(String.class);

      log.warn("BizMsg raw response={}", responseBody);

      if (responseBody == null || responseBody.isBlank()) {
        throw new InfraException(ErrorCode.BIZMSG_API_EMPTY_RESPONSE);
      }

      return parseResponse(responseBody);
    } catch (JsonProcessingException e) {
      log.error("BizMsg response parse failed.", e);
      throw new InfraException(ErrorCode.BIZMSG_API_REQUEST_FAILED, e.getOriginalMessage(), e);
    } catch (RestClientException e) {
      log.error("BizMsg API request failed.", e);
      throw new InfraException(ErrorCode.BIZMSG_API_REQUEST_FAILED, e.getMessage(), e);
    }
  }

  private List<BizMsgSendResponse> parseResponse(String responseBody) throws JsonProcessingException {
    JsonNode root = objectMapper.readTree(responseBody);

    if (root.isArray()) {
      return objectMapper.readValue(responseBody, RESPONSE_TYPE);
    }

    if (!root.isObject()) {
      throw unsupportedResponse(responseBody);
    }

    BizMsgResponseWrapper wrapper = objectMapper.treeToValue(root, BizMsgResponseWrapper.class);

      if (wrapper.data() != null && !wrapper.data().isNull()) {
      if (wrapper.data().isArray()) {
        return objectMapper.convertValue(wrapper.data(), RESPONSE_TYPE);
      }

      if (wrapper.data().isObject()) {
        return List.of(objectMapper.treeToValue(wrapper.data(), BizMsgSendResponse.class));
      }

      if (wrapper.data().isTextual()) {
        return List.of(new BizMsgSendResponse(
            wrapper.code(),
            wrapper.data(),
            resolveWrapperMessage(wrapper),
            null
        ));
      }
    }

    if (looksLikeDirectSendResponse(root)) {
      return List.of(objectMapper.treeToValue(root, BizMsgSendResponse.class));
    }

    throw unsupportedResponse(responseBody);
  }

  private boolean looksLikeDirectSendResponse(JsonNode root) {
    return root.has("code") && (root.has("data") || root.has("message") || root.has("msgid"));
  }

  private String resolveWrapperMessage(BizMsgResponseWrapper wrapper) {
    if (wrapper.error() != null && !wrapper.error().isNull()) {
      return wrapper.message() + " | error=" + wrapper.error();
    }
    return wrapper.message();
  }

  private InfraException unsupportedResponse(String responseBody) {
    log.error("Unsupported BizMsg response body={}", responseBody);
    return new InfraException(
        ErrorCode.BIZMSG_API_REQUEST_FAILED,
        "Unsupported BizMsg response body: " + responseBody
    );
  }
}
