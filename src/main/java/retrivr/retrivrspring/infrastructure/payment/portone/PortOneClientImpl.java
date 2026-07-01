package retrivr.retrivrspring.infrastructure.payment.portone;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentResponse;

@Component
@RequiredArgsConstructor
public class PortOneClientImpl implements PortOneClient {

  private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  private final RestClient.Builder restClientBuilder;
  private final PortOneProperties properties;

  @Override
  public PortOneBillingKeyPaymentResponse chargeBillingKey(
      PortOneBillingKeyPaymentRequest request
  ) {
    PortOneBillingKeyPaymentRequest body = request.withDefaults(
        properties.storeId(),
        properties.channelKey()
    );

    try {
      return restClient().post()
          .uri("/payments/{paymentId}/billing-key", request.paymentId())
          .headers(this::setAuthorization)
          .header(IDEMPOTENCY_KEY, quoteIdempotencyKey())
          .body(body.toPortOneBody())
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new PortOneException("PortOne billing key payment failed. status="
                + res.getStatusCode());
          })
          .body(PortOneBillingKeyPaymentResponse.class);
    } catch (RestClientException e) {
      throw new PortOneException("PortOne billing key payment request failed.", e);
    }
  }

  @Override
  public PortOneScheduleBillingPaymentResponse scheduleBillingPayment(
      PortOneScheduleBillingPaymentRequest request
  ) {
    PortOneScheduleBillingPaymentRequest body = request.withDefaults(
        properties.storeId(),
        properties.channelKey()
    );

    try {
      return restClient().post()
          .uri("/payments/{paymentId}/schedule", request.paymentId())
          .headers(this::setAuthorization)
          .header(IDEMPOTENCY_KEY, quoteIdempotencyKey())
          .body(body.toPortOneBody())
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new PortOneException("PortOne schedule payment failed. status="
                + res.getStatusCode());
          })
          .body(PortOneScheduleBillingPaymentResponse.class);
    } catch (RestClientException e) {
      throw new PortOneException("PortOne schedule payment request failed.", e);
    }
  }

  @Override
  public PortOneCancelScheduledPaymentResponse cancelScheduledPayment(
      PortOneCancelScheduledPaymentRequest request
  ) {
    PortOneCancelScheduledPaymentRequest body = request.withDefaultStoreId(properties.storeId());

    try {
      return restClient().method(HttpMethod.DELETE)
          .uri("/payment-schedules")
          .headers(this::setAuthorization)
          .header(IDEMPOTENCY_KEY, quoteIdempotencyKey())
          .body(body)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new PortOneException("PortOne cancel scheduled payment failed. status="
                + res.getStatusCode());
          })
          .body(PortOneCancelScheduledPaymentResponse.class);
    } catch (RestClientException e) {
      throw new PortOneException("PortOne cancel scheduled payment request failed.", e);
    }
  }

  @Override
  public PortOnePaymentResponse getPayment(String paymentId) {
    try {
      return restClient().get()
          .uri(uriBuilder -> uriBuilder
              .path("/payments/{paymentId}")
              .queryParam("storeId", properties.storeId())
              .build(paymentId))
          .headers(this::setAuthorization)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new PortOneException("PortOne get payment failed. status="
                + res.getStatusCode());
          })
          .body(PortOnePaymentResponse.class);
    } catch (RestClientException e) {
      throw new PortOneException("PortOne get payment request failed.", e);
    }
  }

  private RestClient restClient() {
    return restClientBuilder
        .baseUrl(properties.resolvedBaseUrl())
        .build();
  }

  private void setAuthorization(HttpHeaders headers) {
    headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + properties.apiSecret());
  }

  private String quoteIdempotencyKey() {
    return "\"" + UUID.randomUUID() + "\"";
  }
}
