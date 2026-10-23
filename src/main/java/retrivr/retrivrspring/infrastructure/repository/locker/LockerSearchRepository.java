package retrivr.retrivrspring.infrastructure.repository.locker;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import retrivr.retrivrspring.application.vo.LockerSearchResultWithScore;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;

public interface LockerSearchRepository {

  /**
   * 활성화된 사물함만 대상으로 라벨 및 신청·대여 중인 대여자의 이름·전화번호·이메일을 검색한다.
   * 점수 DESC, 사물함 ID DESC 순서이며 다음 페이지에는 마지막 결과의 두 커서를 함께 전달한다.
   * size만큼 반환하므로 다음 페이지 유무가 필요하면 호출자가 size + 1을 전달한다.
   */
  List<LockerSearchResultWithScore> searchLockerPageBy(
      Long organizationId, String keyword, UUID cursorLockerId, Double cursorScore, int size);

  /** 두 대표 사물함의 그룹 사이에 상하좌우로 인접한 구성원이 있는지 확인한다. */
  boolean existsAdjacentGroups(Locker target, Locker parent);
}
