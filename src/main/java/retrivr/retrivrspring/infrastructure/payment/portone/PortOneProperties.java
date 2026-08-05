package retrivr.retrivrspring.infrastructure.payment.portone;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone.v2")
public record PortOneProperties(
    String storeId,
    String baseUrl,
    String apiSecret,
    PortOneKakaoPayProperties kakaoPayProperties,
    PortOneTossPayProperties tossPayProperties,
    PortOneKGInicisProperties kgInicisProperties
) {

  public String resolvedBaseUrl() {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "https://api.portone.io";
    }
    return baseUrl;
  }
}
