package retrivr.retrivrspring.domain.entity.rental.enumerate;

public enum RentalStatus {
  REQUESTED,   // 승인 대기
  APPROVED,    // 대여 중
  REJECTED,    // 거부됨
  RETURNED,    // 반납 완료
  OVERDUE      // 연체
}
