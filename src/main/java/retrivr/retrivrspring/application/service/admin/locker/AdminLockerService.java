package retrivr.retrivrspring.application.service.admin.locker;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.application.vo.LockerSearchResultWithScore;
import retrivr.retrivrspring.domain.entity.locker.*;
import retrivr.retrivrspring.domain.entity.locker.enumerate.*;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.locker.*;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerAvailabilityRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerCombinationRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerEnableRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerLabelUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.LockerRentalAbleStatus;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerMutationResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerSearchPageResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLockerService {

  private final OrganizationRepository organizationRepository;
  private final LockerRepository lockerRepository;
  private final LockerRentalRepository rentalRepository;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AdminLockerSearchPageResponse search(String keyword, UUID cursorLockerId, Double cursorScore,
      Integer size, Long loginOrganizationId) {
    validateSearchCursor(cursorLockerId, cursorScore);
    Organization organization = findOrganization(loginOrganizationId);
    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(size);

    List<LockerSearchResultWithScore> lockers = lockerRepository.searchLockerPageBy(
        loginOrganizationId, keyword, cursorLockerId, cursorScore, normalizedSize.sizePlusOne());

    boolean hasNext = lockers.size() > normalizedSize.size();
    List<LockerSearchResultWithScore> page = hasNext ? lockers.subList(0, normalizedSize.size()) : lockers;
    if (page.isEmpty()) {
      return new AdminLockerSearchPageResponse(List.of(), null, null);
    }

    Double nextScoreCursor = hasNext ? page.getLast().score() : null;
    UUID nextLockerIdCursor = hasNext ? page.getLast().lockerId() : null;
    List<UUID> lockerIds = page.stream()
        .map(LockerSearchResultWithScore::lockerId)
        .toList();

    Map<UUID, Locker> lockerMap = lockerRepository.findAllByIdInAndZoneOrganization(lockerIds, organization)
        .stream().collect(Collectors.toMap(Locker::getId, locker -> locker));
    Map<UUID, LockerRental> rentalMap = new HashMap<>();
    rentalRepository.findAllByLocker_IdInAndStatusInOrderByRequestedAtDescIdDesc(lockerIds, LockerRentalStatus.ACTIVE_RENTALS)
        .forEach(rental -> rentalMap.putIfAbsent(rental.getLocker().getId(), rental));

    // IN 조회 결과 순서가 아닌 검색 점수 순서대로 응답을 구성한다.
    List<AdminLockerSearchPageResponse.LockerSearchSummary> summary = page.stream()
        .map(result -> lockerMap.get(result.lockerId()))
        .filter(Objects::nonNull)
        .map(locker -> AdminLockerSearchPageResponse.LockerSearchSummary.from(locker, rentalMap.get(locker.getId())))
        .toList();

    return new AdminLockerSearchPageResponse(summary, nextScoreCursor, nextLockerIdCursor);
  }

  @Transactional
  public AdminLockerMutationResponse updateEnable(Long organizationId, UUID lockerId, AdminLockerEnableRequest request) {
    Organization organization = lockOrganization(organizationId);
    Locker locker = findLocker(organization, lockerId);

    if (request.enableStatus() == LockerEnableStatus.DISABLED) {
      assertNoActiveRentalInLocker(locker);
      locker.disable();
    }
    else {
      locker.enable();
    }

    return mutationResponse(locker);
  }

  @Transactional
  public AdminLockerMutationResponse updateRentalAble(Long organizationId, UUID lockerId,
      AdminLockerAvailabilityRequest request) {
    Organization organization = lockOrganization(organizationId);
    Locker locker = findLocker(organization, lockerId);
    assertNoActiveRentalInLocker(locker);

    if (request.rentalAbleStatus() == LockerRentalAbleStatus.RENTAL_UNABLE) {
      locker.disableRental();
    }
    else {
      locker.enableRental();
    }

    return mutationResponse(locker);
  }

  @Transactional
  public AdminLockerMutationResponse updateLabel(Long organizationId, UUID lockerId,
      AdminLockerLabelUpdateRequest request) {
    Organization organization = lockOrganization(organizationId);
    Locker locker = findLocker(organization, lockerId);
    if (!locker.getLabel().equals(request.label())) {
      if (lockerRepository.existsByZoneAndLabelAndIdNot(locker.getZone(), request.label(), lockerId)) {
        throw new ApplicationException(ErrorCode.DUPLICATE_LOCKER_LABEL);
      }
      locker.updateLabel(request.label());
    }
    return mutationResponse(locker);
  }

  /**
   * 공통 로직
   */
  private void validateSearchCursor(UUID cursorLockerId, Double cursorScore) {
    if ((cursorLockerId == null) != (cursorScore == null)) {
      throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
          "사물함 ID 커서와 점수 커서는 함께 전달해야 합니다.");
    }
    if (cursorScore != null && (!Double.isFinite(cursorScore) || cursorScore < 0)) {
      throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
          "점수 커서는 유한한 0 이상의 값이어야 합니다.");
    }
  }

  private AdminLockerMutationResponse mutationResponse(Locker locker) {
    lockerRepository.flush();
    return new AdminLockerMutationResponse(locker.getZone().getId(), locker.getZone().getVersion(),
        AdminLockerResponse.from(locker));
  }

  private Organization lockOrganization(Long organizationId) {
    // 향후 대여 생성도 동일한 조직 잠금을 획득해 배치 변경과의 경쟁을 직렬화해야 한다.
    return organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private Organization findOrganization(Long organizationId) {
    return organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private Locker findLocker(Organization organization, UUID lockerId) {
    return lockerRepository.findByIdAndZoneOrganization(lockerId, organization)
        .orElseThrow(() -> new ApplicationException(ErrorCode.LOCKER_NOT_FOUND));
  }

  private void assertNoActiveRentalInLocker(Locker locker) {
    if (rentalRepository.existsByLockerAndStatusIn(locker, LockerRentalStatus.ACTIVE_RENTALS)) {
      throw new ApplicationException(ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS);
    }
  }
}
