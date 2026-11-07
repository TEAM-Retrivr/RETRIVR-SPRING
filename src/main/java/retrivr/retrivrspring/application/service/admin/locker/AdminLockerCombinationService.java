package retrivr.retrivrspring.application.service.admin.locker;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.locker.LockerRentalRepository;
import retrivr.retrivrspring.domain.repository.locker.LockerRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.locker.req.locker.AdminLockerCombinationRequest;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerMutationResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerResponse;

@Service
@RequiredArgsConstructor
public class AdminLockerCombinationService {

  private final LockerRepository lockerRepository;
  private final OrganizationRepository organizationRepository;
  private final LockerRentalRepository rentalRepository;

  @Transactional
  public AdminLockerMutationResponse combination(Long organizationId,
      AdminLockerCombinationRequest request) {
    Organization organization = lockOrganization(organizationId);
    Locker target = findLocker(request.targetLockerId());
    Locker parent = findLocker(request.parentLockerId());
    // 소유 권한 검증
    target.validateOwner(organization);
    parent.validateOwner(organization);
    // Enable 상태인지 검증
    target.validateEnable();
    parent.validateEnable();
    // 각 Locker 가 부모 Locker 인지 검증
    target.validateRepresentativeLocker();
    parent.validateRepresentativeLocker();
    // target 과 parent 가 동일한지 검증
    if (request.targetLockerId().equals(request.parentLockerId())) {
      throw new ApplicationException(ErrorCode.LOCKER_SELF_COMBINATION_NOT_ALLOWED);
    }
    // 대여중/요청중인 상태인지 검증
    assertNoActiveRentalInLocker(target);
    assertNoActiveRentalInLocker(parent);
    // parent와 target 이 연결 가능한지 검증
    if (!lockerRepository.existsAdjacentGroups(target, parent)) {
      throw new ApplicationException(ErrorCode.LOCKER_NOT_ADJACENT);
    }

    target.combine(parent);
    lockerRepository.updateByParentLocker(target, parent);

    return new AdminLockerMutationResponse(
        target.getZone().getId(),
        target.getZone().getVersion(),
        AdminLockerResponse.from(target)
    );
  }

  private Organization lockOrganization(Long organizationId) {
    return organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private Locker findLocker(UUID lockerId) {
    return lockerRepository.findById(lockerId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.LOCKER_NOT_FOUND));
  }

  private void assertNoActiveRentalInLocker(Locker locker) {
    if (rentalRepository.existsByLockerAndStatusIn(locker, LockerRentalStatus.ACTIVE_RENTALS)) {
      throw new ApplicationException(ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS);
    }
  }
}
