package retrivr.retrivrspring.infrastructure.payment.portone;

import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneBillingKeyPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneCancelScheduledPaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOnePaymentResponse;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentRequest;
import retrivr.retrivrspring.infrastructure.payment.portone.data.PortOneScheduleBillingPaymentResponse;

public interface PortOneClient {

  PortOneBillingKeyPaymentResponse chargeBillingKey(PortOneBillingKeyPaymentRequest request);

  PortOneScheduleBillingPaymentResponse scheduleBillingPayment(
      PortOneScheduleBillingPaymentRequest request
  );

  PortOneCancelScheduledPaymentResponse cancelScheduledPayment(
      PortOneCancelScheduledPaymentRequest request
  );

  PortOnePaymentResponse getPayment(String paymentId);
}
