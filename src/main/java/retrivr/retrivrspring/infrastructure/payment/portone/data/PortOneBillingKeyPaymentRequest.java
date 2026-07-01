package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneBillingKeyPaymentRequest(
    String paymentId,
    String storeId,
    String billingKey,
    String channelKey,
    String orderName,
    PortOneCustomerRequest customer,
    String customData,
    PortOnePaymentAmountRequest amount,
    String currency,
    List<String> noticeUrls,
    List<PortOnePaymentProductRequest> products,
    Integer productCount,
    Boolean skipWebhook
) {

  public PortOneBillingKeyPaymentRequest withDefaults(String defaultStoreId, String defaultChannelKey) {
    return new PortOneBillingKeyPaymentRequest(
        paymentId,
        storeId != null ? storeId : defaultStoreId,
        billingKey,
        channelKey != null ? channelKey : defaultChannelKey,
        orderName,
        customer,
        customData,
        amount,
        currency,
        noticeUrls,
        products,
        productCount,
        skipWebhook
    );
  }

  @JsonIgnore
  public PortOneBillingKeyPaymentBody toPortOneBody() {
    return new PortOneBillingKeyPaymentBody(
        storeId,
        billingKey,
        channelKey,
        orderName,
        customer,
        customData,
        amount,
        currency,
        noticeUrls,
        products,
        productCount,
        skipWebhook
    );
  }

  @Override
  public String toString() {
    return "PortOneBillingKeyPaymentRequest[paymentId=" + paymentId
        + ", orderName=" + orderName
        + ", amount=" + amount
        + ", currency=" + currency
        + "]";
  }
}
