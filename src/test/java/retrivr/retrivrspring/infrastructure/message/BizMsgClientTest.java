package retrivr.retrivrspring.infrastructure.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import retrivr.retrivrspring.global.properties.AlimTalkProperties;

class BizMsgClientTest {

  @Test
  @DisplayName("send: parses wrapped object response")
  void send_parsesWrappedObjectResponse() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    BizMsgClient client = new BizMsgClient(
        new AlimTalkProperties("https://bizmsg.test", "test-user", "profile-key"),
        builder,
        new ObjectMapper()
    );

    server.expect(requestTo("https://bizmsg.test/v2/sender/send"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("userid", "test-user"))
        .andExpect(header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess("""
            {
              "code": "success",
              "message": "ok",
              "data": [
                {
                  "code": "success",
                  "data": "accepted",
                  "message": "sent",
                  "type": "AT",
                  "msgid": "msg-123"
                }
              ]
            }
            """, MediaType.APPLICATION_JSON));

    List<BizMsgSendResponse> responses = client.send(List.of(new BizMsgSendRequest(
        "AT",
        "01012345678",
        "profile-key",
        "tmpl-1",
        "title",
        "message",
        "00000000000000"
    )));

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().msgid()).isEqualTo("msg-123");
    assertThat(responses.getFirst().isSuccess()).isTrue();
  }

  @Test
  @DisplayName("send: parses array response")
  void send_parsesArrayResponse() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    BizMsgClient client = new BizMsgClient(
        new AlimTalkProperties("https://bizmsg.test", "test-user", "profile-key"),
        builder,
        new ObjectMapper()
    );

    server.expect(requestTo("https://bizmsg.test/v2/sender/send"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            [
              {
                "code": "success",
                "data": "accepted",
                "message": "sent",
                "type": "AT",
                "msgid": "msg-123"
              }
            ]
            """, MediaType.APPLICATION_JSON));

    List<BizMsgSendResponse> responses = client.send(List.of(new BizMsgSendRequest(
        "AT",
        "01012345678",
        "profile-key",
        "tmpl-1",
        "title",
        "message",
        "00000000000000"
    )));

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().msgid()).isEqualTo("msg-123");
    assertThat(responses.getFirst().isSuccess()).isTrue();
  }
}
