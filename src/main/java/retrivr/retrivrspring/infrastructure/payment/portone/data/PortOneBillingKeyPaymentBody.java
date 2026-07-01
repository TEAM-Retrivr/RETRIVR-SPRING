package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneBillingKeyPaymentBody(
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
}
