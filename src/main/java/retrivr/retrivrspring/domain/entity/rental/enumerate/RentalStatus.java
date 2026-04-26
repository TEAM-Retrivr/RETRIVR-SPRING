package retrivr.retrivrspring.domain.entity.rental.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RentalStatus {
  REQUESTED("승인 대기"),   // 승인 대기
  RENTED("대여 중"),    // 대여 중
  REJECTED("거부됨"),    // 거부됨
  RETURNED("반납 완료");    // 반납 완료

  private final String korean;
}
