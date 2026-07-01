package retrivr.retrivrspring.infrastructure.payment.portone;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone.v2")
public record PortOneProperties(
    String storeId,
    String channelKey,
    String apiSecret,
    String baseUrl
) {

  public String resolvedBaseUrl() {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "https://api.portone.io";
    }
    return baseUrl;
  }
}
