package retrivr.retrivrspring.application.service.admin.rental;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.event.RentalReturnedEvent;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.application.vo.RentedRentalSearchResultWithScore;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.message.MessageSendStatus;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.repository.message.MessageHistoryRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.admin.rental.req.AdminRentalReturnRequest;
import retrivr.retrivrspring.presentation.admin.rental.req.AdminRentalDueDateUpdateRequest;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminOverdueRentalItemPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminOverdueRentalItemPageResponse.OverdueRentalItemSummary;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalDueDateUpdateResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalItemPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalItemPageResponse.RentalItemSummary;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalReturnResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalSearchPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalSearchPageResponse.RentalSearchSummary;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminReturnItemUnitListPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminReturnItemUnitListPageResponse.BorrowedItemSummary;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminActiveRentalService {

  private final RentalRepository rentalRepository;
  private final MessageHistoryRepository messageHistoryRepository;
  private final OrganizationRepository organizationRepository;
  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * 연체된 물품 리스트 조회 (status 기반)
   */
  public AdminOverdueRentalItemPageResponse getOverdueItemList(Long organizationId, Long cursor,
      int size) {

    LocalDate today = LocalDate.now();
    long offset = cursor != null ? cursor : 0L;

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        size);

    List<Rental> rentals = rentalRepository.searchOverduePageVerified(organizationId, cursor,
        normalizedSize.sizePlusOne(), today);

    boolean hasNext = rentals.size() > normalizedSize.size();
    List<Rental> page = hasNext ? rentals.subList(0, normalizedSize.size()) : rentals;
    Long nextCursor = null;
    if (hasNext) {
      nextCursor = offset + page.size();
    }

    Map<Long, LocalDate> lastSentOverdueReminderDateByRentalId =
        messageHistoryRepository.findByRentalInAndMessageTypeAndStatusOrderBySentDateDesc(
                page,
                MessageType.OVERDUE_REMINDER,
                MessageSendStatus.SUCCESS
            ).stream()
            .collect(Collectors.toMap(
                messageHistory -> messageHistory.getRental().getId(),
                messageHistory -> messageHistory.getSentDate(),
                (existing, ignored) -> existing
            ));

    List<OverdueRentalItemSummary> rows = page.stream()
        .map(rental -> OverdueRentalItemSummary.from(
            rental,
            lastSentOverdueReminderDateByRentalId.get(rental.getId())
        ))
        .toList();

    return new AdminOverdueRentalItemPageResponse(rows, nextCursor);
  }

  /**
   * 반납 화면에서의 물품 리스트 조회 (아이템 요약)
   */
  public AdminRentalItemPageResponse getRentalItemSummaryList(Long organizationId, Long cursor,
      int size) {

    boolean isValidOrganization = organizationRepository.existsById(organizationId);
    if (!isValidOrganization) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION);
    }

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        size);

    List<Item> items = itemRepository.findPageByOrganizationWithCursor(organizationId, cursor,
        normalizedSize.sizePlusOne());

    boolean hasNext = items.size() > normalizedSize.size();
    List<Item> page = hasNext ? items.subList(0, normalizedSize.size()) : items;

    Long nextCursor = null;
    if (hasNext) {
      nextCursor = page.getLast().getId();
    }

    List<RentalItemSummary> rows = page.stream()
        .map(RentalItemSummary::from)
        .toList();

    return new AdminRentalItemPageResponse(rows, nextCursor);
  }

  /**
   * 대여 중인 물품 상세 조회 (특정 itemId의 active rentals / units)
   */
  public AdminReturnItemUnitListPageResponse getReturnDetail(
      Long organizationId,
      Long itemId,
      Long cursor,
      int size
  ) {

    Item item = itemRepository.findById(itemId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));
    validateItemOwner(item, organizationId);

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        size);
    Long nextCursor = null;
    List<BorrowedItemSummary> rows;

    if (item.isUnitType()) {
      List<ItemUnit> units = itemUnitRepository.searchRentedUnitsByItemId(itemId, cursor,
          normalizedSize.sizePlusOne());
      Map<Long, Rental> rentalItems = rentalRepository.findByItemUnitIn(units);

      boolean hasNext = units.size() > normalizedSize.size();
      List<ItemUnit> page = hasNext ? units.subList(0, normalizedSize.size()) : units;
      if (hasNext) {
        nextCursor = page.getLast().getId();
      }

      rows = page.stream()
          .map(itemUnit -> BorrowedItemSummary.fromUnit(itemUnit, rentalItems.get(itemUnit.getId())))
          .toList();
    } else {
      List<Rental> rentals = rentalRepository.findRentedByItemId(itemId, cursor,
          normalizedSize.sizePlusOne());

      boolean hasNext = rentals.size() > normalizedSize.size();
      List<Rental> page = hasNext ? rentals.subList(0, normalizedSize.size()) : rentals;
      if (hasNext) {
        nextCursor = page.getLast().getId();
      }

      rows = page.stream()
          .map(rental -> BorrowedItemSummary.fromNonUnit(item, rental))
          .toList();
    }

    return new AdminReturnItemUnitListPageResponse(
        item.getId(),
        item.getName(),
        item.getGuaranteedGoods(),
        item.getAvailableQuantity(),
        item.getTotalQuantity(),
        item.getRentalDuration(),
        rows,
        nextCursor
    );
  }

  /**
   * 반납 확인 - status: RENTED -> RETURNED - UNIT: rental_item_unit으로 점유한 유닛 AVAILABLE 복구 -
   * SINGLE: rental_item.quantity 합산해서 item.available_quantity 증가 - return_event 기록
   */
  @Transactional
  public AdminRentalReturnResponse confirmReturn(
      Long organizationId,
      Long rentalId,
      AdminRentalReturnRequest request
  ) {
    // 1. 대여 정보 조회
    Rental rental = rentalRepository.findByIdWithItems(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    // 2. 로그인된 조직 정보 조회
    Organization loginOrganization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    // 3. 반납 처리
    rental.markReturned(request.adminNameToConfirm(), loginOrganization);
    applicationEventPublisher.publishEvent(new RentalReturnedEvent(rental.getId()));

    return new AdminRentalReturnResponse(rentalId, RentalStatus.RETURNED,
        request.adminNameToConfirm());
  }

  /**
   * 반납 예정일 수정 (이력 테이블 X, dueDate 자체 수정)
   */
  @Transactional
  public AdminRentalDueDateUpdateResponse updateDueDate(
      Long loginOrganizationId,
      Long rentalId,
      AdminRentalDueDateUpdateRequest request
  ) {
    // 1. 대여 정보 조회
    Rental rental = rentalRepository.findFetchOrganizationById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    // 2. 로그인한 조직 정보 조회
    Organization loginOrganization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    // 3. DueDate 변경
    rental.changeDueDate(request.newReturnDueDate(), loginOrganization);

    return new AdminRentalDueDateUpdateResponse(rentalId, rental.getDueDate());
  }

  // -------------------------
  // mapping / validation
  // -------------------------

  private void validateItemOwner(Item item, Long organizationId) {
    if (!item.getOrganization().getId().equals(organizationId)) {
      throw new ApplicationException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
    }
  }

  public AdminRentalSearchPageResponse searchRankedPageByKeyword(String keyword, Long cursorRentalId, Double cursorScore,
      Integer size, Long loginOrganizationId) {
    Organization loginOrganization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        size);

    List<RentedRentalSearchResultWithScore> rentals = rentalRepository.searchRentedRentalPageBy(loginOrganizationId, keyword,
        cursorRentalId, cursorScore, normalizedSize.sizePlusOne());

    boolean hasNext = rentals.size() > normalizedSize.size();
    List<RentedRentalSearchResultWithScore> page = hasNext ? rentals.subList(0, normalizedSize.size()) : rentals;
    Double nextScoreCursor = null;
    Long nextRentalIdCursor = null;
    if (hasNext) {
      nextScoreCursor = page.getLast().score();
      nextRentalIdCursor = page.getLast().rentalId();
    }

    List<Long> rentalIds = page.stream()
        .map(RentedRentalSearchResultWithScore::rentalId)
        .toList();

    Map<Long, Rental> rentalMap = rentalRepository.findFetchBorrowerAndItemByIdIn(rentalIds).stream()
        .collect(Collectors.toMap(Rental::getId, rental -> rental));

    List<RentalSearchSummary> summary = page.stream()
        .map(r -> rentalMap.get(r.rentalId()))
        .filter(java.util.Objects::nonNull)
        .map(RentalSearchSummary::from)
        .toList();

    return new AdminRentalSearchPageResponse(
        summary,
        nextScoreCursor,
        nextRentalIdCursor
    );
  }
}
