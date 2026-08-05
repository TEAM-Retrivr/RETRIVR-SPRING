package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneScheduleBillingPaymentRequest(
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
    OffsetDateTime timeToPay
) {

  public PortOneScheduleBillingPaymentRequest withDefaults(
      String defaultStoreId,
      String defaultChannelKey
  ) {
    return new PortOneScheduleBillingPaymentRequest(
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
        timeToPay
    );
  }

  @JsonIgnore
  public PortOneScheduleBillingPaymentBody toPortOneBody() {
    PortOneBillingKeyPaymentBody payment = new PortOneBillingKeyPaymentBody(
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
        null
    );
    return new PortOneScheduleBillingPaymentBody(payment, timeToPay);
  }

  @Override
  public String toString() {
    return "PortOneScheduleBillingPaymentRequest[paymentId=" + paymentId
        + ", orderName=" + orderName
        + ", amount=" + amount
        + ", currency=" + currency
        + ", timeToPay=" + timeToPay
        + "]";
  }
}
