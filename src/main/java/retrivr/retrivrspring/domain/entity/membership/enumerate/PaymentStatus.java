package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PaymentStatus {
  PENDING("결제 대기"),
  UNKNOWN("결제 결과 확인 필요"),
  SUCCESS("결제 성공"),
  COMPENSATION_REQUIRED("결제 보상 필요"),
  REFUND_PROCESSING("환불 처리 중"),
  REFUND_UNKNOWN("환불 결과 확인 필요"),
  REFUND_FAILED("환불 실패"),
  REFUNDED("환불 완료"),
  FAILED("결제 실패"),
  SCHEDULE_PENDING("결제 예약 대기"),
  SCHEDULE_UNKNOWN("결제 예약 결과 확인 필요"),
  SCHEDULED("결제 예약"),
  SCHEDULE_CANCELED("결제 예약 취소");

  private final String korean;
}
