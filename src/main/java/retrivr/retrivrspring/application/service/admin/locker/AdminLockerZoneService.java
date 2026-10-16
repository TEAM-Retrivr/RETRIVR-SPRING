package retrivr.retrivrspring.application.service.admin.locker;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.locker.Locker;
import retrivr.retrivrspring.domain.entity.locker.LockerZone;
import retrivr.retrivrspring.domain.entity.locker.enumerate.LockerRentalStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.locker.LockerRentalRepository;
import retrivr.retrivrspring.domain.repository.locker.LockerRepository;
import retrivr.retrivrspring.domain.repository.locker.LockerZoneRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneCreateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneNameUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneOrderRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.AdminLockerZoneLayoutUpdateRequest;
import retrivr.retrivrspring.presentation.admin.locker.req.zone.LockerFilteringStatus;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerLayoutResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.locker.AdminLockerResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerSummaryResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneDeleteResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneListResponse;
import retrivr.retrivrspring.presentation.admin.locker.res.zone.AdminLockerZoneResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLockerZoneService {

  private static final List<LockerRentalStatus> ACTIVE_RENTALS =
      List.of(LockerRentalStatus.REQUESTED, LockerRentalStatus.RENTED);
  private final OrganizationRepository organizationRepository;
  private final LockerZoneRepository zoneRepository;
  private final LockerRepository lockerRepository;
  private final LockerRentalRepository rentalRepository;

  public AdminLockerZoneListResponse getZones(Long organizationId) {
    Organization organization = findOrganization(organizationId);
    return new AdminLockerZoneListResponse(
        zones(organization).stream()
            .map(AdminLockerZoneResponse::from)
            .toList()
    );
  }

  @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public AdminLockerLayoutResponse getLockers(Long organizationId, LockerFilteringStatus filter, UUID zoneId) {
    Organization organization = findOrganization(organizationId);
    LockerZone zone = findZone(organization, zoneId);

    List<Locker> lockers;
    if (filter == null) {
      lockers = lockers(zone);
    }
    else {
      lockers = lockerRepository.findByZoneAndOperationStatusOrderByRowIndexAscColumnIndexAsc(zone, filter.toLockerOperationStatus());
    }
    return new AdminLockerLayoutResponse(
        AdminLockerZoneResponse.from(zone),
        lockers.stream().map(AdminLockerResponse::from).toList());
  }

  public AdminLockerSummaryResponse getLockersSummary(Long organizationId, UUID zoneId) {
    findOrganization(organizationId);
    return AdminLockerSummaryResponse.from(lockerRepository.countByStatus(organizationId, zoneId.toString()));
  }

  @Transactional
  public AdminLockerZoneResponse createZone(Long organizationId, AdminLockerZoneCreateRequest request) {
    Organization organization = lockOrganization(organizationId);
    // 최대 생성 가능 갯수 검증
    if (LockerZone.MAX_ZONE_COUNT < zoneRepository.countByOrganization(organization) + 1) {
      throw new ApplicationException(ErrorCode.LOCKER_ZONE_LIMIT_EXCEEDED);
    }
    // 이름 중복 검증
    if (zoneRepository.existsByOrganizationAndName(organization, request.name())) {
      throw new ApplicationException(ErrorCode.DUPLICATE_LOCKER_ZONE_NAME);
    }

    int sortOrder = zoneRepository.maximumSortOrder(organizationId) + 1;

    LockerZone zone = zoneRepository.saveAndFlush(
        LockerZone.create(organization, request.name(), sortOrder, request.rowCount(), request.columnCount())
    );
    return AdminLockerZoneResponse.from(zone);
  }

  @Transactional
  public AdminLockerZoneResponse updateZoneLayout(Long organizationId, UUID zoneId, AdminLockerZoneLayoutUpdateRequest request) {
    Organization organization = lockOrganization(organizationId);
    LockerZone zone = findZone(organization, zoneId);
    zone.validateVersion(request.version());

    boolean resized = zone.getRowCount() != request.rowCount() || zone.getColumnCount() != request.columnCount();
    if (resized) {
      assertNoActiveRentalInZone(zone);
      //영속성 컨텍스트 초기화 -> locker, zone 에 대한 save 필요
      deleteLockers(zone);
      zone.updateSize(request.rowCount(), request.columnCount());
      zone.createLockerLayout();
      lockerRepository.saveAll(zone.getLockers());
      zoneRepository.save(zone);
    }
    return AdminLockerZoneResponse.from(zone);
  }

  @Transactional
  public AdminLockerZoneResponse updateZoneName(Long organizationId, UUID zoneId, AdminLockerZoneNameUpdateRequest request) {
    Organization organization = lockOrganization(organizationId);
    LockerZone zone = findZone(organization, zoneId);
    zone.validateVersion(request.version());

    if (request.name().equals(zone.getName())) {
      return AdminLockerZoneResponse.from(zone);
    }

    if (zoneRepository.existsByOrganizationAndNameAndIdNot(organization, request.name(), zoneId)) {
      throw new ApplicationException(ErrorCode.DUPLICATE_LOCKER_ZONE_NAME);
    }

    zone.updateName(request.name());
    return AdminLockerZoneResponse.from(zone);
  }

  @Transactional
  public AdminLockerZoneListResponse reorderZones(Long organizationId, AdminLockerZoneOrderRequest request) {
    Organization organization = lockOrganization(organizationId);
    List<LockerZone> zones = zones(organization);

    List<UUID> reqIds = request.zoneIds();
    Set<UUID> reqIdsSet = new HashSet<>(reqIds);
    Set<UUID> expectedIds = new HashSet<>(
        zones.stream()
        .map(LockerZone::getId)
        .toList()
    );
    Map<UUID, LockerZone> expectedLockerZones = zones.stream()
        .collect(Collectors.toMap(
            LockerZone::getId,
            Function.identity()
        ));

    // request 검증
    if (reqIds.size() != expectedIds.size()
        || reqIdsSet.size() != reqIds.size()
        || !expectedIds.equals(reqIdsSet)) {
      throw new ApplicationException(ErrorCode.INVALID_LOCKER_ZONE_ORDER);
    }
    if (zones.isEmpty()) {
      return new AdminLockerZoneListResponse(List.of());
    }

    // unique 제약을 피하기 위해 존재하지 않는 sortOrder 로 미리 변경
    int minSortOrder = zones.getFirst().getSortOrder();
    for (int order = 1; order <= zones.size(); order++) {
      zones.get(order - 1).updateSortOrder(minSortOrder - order);
    }
    zoneRepository.saveAllAndFlush(zones);

    // sortOrder 변경
    for (int order = 1; order <= zones.size(); order++) {
      expectedLockerZones.get(reqIds.get(order - 1)).updateSortOrder(order);
    }
    zones.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

    return new AdminLockerZoneListResponse(zones.stream()
        .map(AdminLockerZoneResponse::from)
        .toList()
    );
  }

  @Transactional
  public AdminLockerZoneDeleteResponse deleteZone(Long organizationId, UUID zoneId) {
    Organization organization = lockOrganization(organizationId);
    LockerZone zone = findZone(organization, zoneId);
    assertNoActiveRentalInZone(zone);
    deleteZone(zone);
    return new AdminLockerZoneDeleteResponse(zoneId, true);
  }

  /**
   * 공통 DB 컨트롤 로직
   */
  private Organization lockOrganization(Long organizationId) {
    // 향후 대여 생성도 동일한 조직 잠금을 획득해 배치 변경과의 경쟁을 직렬화해야 한다.
    return organizationRepository.findByIdForUpdate(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private Organization findOrganization(Long organizationId) {
    return organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private LockerZone findZone(Organization organizationId, UUID zoneId) {
    return zoneRepository.findByIdAndOrganization(zoneId, organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.LOCKER_ZONE_NOT_FOUND));
  }

  private List<LockerZone> zones(Organization organization) {
    return zoneRepository.findAllByOrganizationOrderBySortOrderAsc(organization);
  }

  private List<Locker> lockers(LockerZone zone) {
    return lockerRepository.findAllByZoneOrderByRowIndexAscColumnIndexAsc(zone);
  }

  private void assertNoActiveRentalInZone(LockerZone zone) {
    if (rentalRepository.existsByLockerZoneAndStatusIn(zone, ACTIVE_RENTALS)) {
      throw new ApplicationException(ErrorCode.LOCKER_ACTIVE_RENTAL_EXISTS);
    }
  }

  private void deleteZone(LockerZone zone) {
    deleteLockers(zone);
    zoneRepository.delete(zone);
  }

  private void deleteLockers(LockerZone zone) {
    rentalRepository.clearLockerByZone(zone);
    lockerRepository.deleteAllByZoneId(zone);
  }
}
