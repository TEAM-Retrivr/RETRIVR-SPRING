package retrivr.retrivrspring.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonConfigTest {

  private final ObjectMapper objectMapper;

  JacksonConfigTest() {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    new JacksonConfig().jsonCustomizer().customize(builder);
    this.objectMapper = builder.build();
  }

  @Test
  void localDateTimeIsSerializedAsYearMonthDayHourMinute() throws Exception {
    DateTimeResponse response = new DateTimeResponse(LocalDateTime.of(2026, 1, 21, 17, 0, 59));

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).isEqualTo("{\"requestedAt\":\"2026-01-21 17:00\"}");
  }

  private record DateTimeResponse(LocalDateTime requestedAt) {
  }
}
