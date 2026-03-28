package retrivr.retrivrspring.application.service.open;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.event.RentalRequestedEvent;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalDetailResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicRentalService {

  private final ObjectMapper objectMapper;
  private final RentalRepository rentalRepository;
  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public PublicRentalCreateResponse requestRental(Long itemId, PublicRentalCreateRequest request) {
    // 1. 대여할 Item 조회
    Item targetItem = itemRepository.findFetchItemBorrowerFieldsById(itemId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

    // 2. ItemUnit 조회
    ItemUnit targetItemUnit = null;
    if (request.itemUnitId() != null) {
      targetItemUnit = itemUnitRepository.findById(request.itemUnitId())
          .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM_UNIT));
    }

    // 3. itemBorrower Field 를 통해서 request.rentalFields를 검증
    targetItem.validationItemBorrowerFieldsWith(request.renterFields());

    // 4. Borrower 생성
    Borrower borrower = Borrower.create(
        request.name(),
        new PhoneNumber(request.phone()),
        objectMapper.valueToTree(request.renterFields())
    );

    // 5. Rental 생성 및 저장
    Rental requestedRental = Rental.request(targetItem, targetItemUnit, borrower);
    rentalRepository.save(requestedRental);
    applicationEventPublisher.publishEvent(new RentalRequestedEvent(requestedRental.getId()));

    return new PublicRentalCreateResponse(requestedRental.getId(), targetItem.getId(),
        request.itemUnitId(), requestedRental.getRequestedAt());
  }

  public PublicRentalDetailResponse checkRentalStatusAndDetail(Long rentalId) {
    Rental rental = rentalRepository.findById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    //todo: 장바구니? 기능 이후 name List 를 넘기도록 수정
    String itemName = rental.getRentalItems().getFirst().getItem().getName();
    String itemUnitLabel = null;
    if (!rental.getRentalItemUnits().isEmpty()) {
      itemUnitLabel = rental.getRentalItemUnits().getFirst().getItemUnit().getLabel();
    }

    Map<String, String> borrowerField = new HashMap<>();
    if (rental.getBorrower().hasAdditionalInfo()) {
      borrowerField = objectMapper.convertValue(
          rental.getBorrower().getAdditionalBorrowerInfo(),
          new TypeReference<Map<String, String>>() {}
      );
    }

    return PublicRentalDetailResponse.from(rental, itemName, itemUnitLabel, borrowerField);
  }
}
