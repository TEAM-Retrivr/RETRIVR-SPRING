package retrivr.retrivrspring.domain.entity.membership.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PaymentStatus {
  SUCCESS("결제 성공"),
  FAILED("결제 실패");

  private final String korean;
}
