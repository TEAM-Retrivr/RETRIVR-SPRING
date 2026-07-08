package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import retrivr.retrivrspring.domain.entity.membership.enumerate.PaymentProvider;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneSelectedChannelResponse(
    String type,
    String id,
    String key,
    String name,
    String pgProvider,
    String pgMerchantId
) {
  public PaymentProvider resolveProvider() {
    return PaymentProvider.valueOf(pgProvider.toUpperCase());
  }
}
