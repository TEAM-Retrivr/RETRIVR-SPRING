package retrivr.retrivrspring.application.vo;

public enum BillingResult {
  NOT_SUBSCRIBED,      // 구독 없음/비활성
  PAYMENT_SUCCEEDED,  // 결제 성공
  PAYMENT_RETRYABLE_FAILED, // 일시 실패, 기존 pass 유지
  PAYMENT_FINAL_FAILED      // 최종 실패, 기존 pass 만료 가능
}
