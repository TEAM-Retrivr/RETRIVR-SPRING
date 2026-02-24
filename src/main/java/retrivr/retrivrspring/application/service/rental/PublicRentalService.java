package retrivr.retrivrspring.application.service.rental;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.item.ItemRepository;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.infrastructure.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalDetailResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicRentalService {

  private final ObjectMapper objectMapper;
  private final RentalRepository rentalRepository;
  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;

  @Transactional
  public PublicRentalCreateResponse requestRental(Long itemId, PublicRentalCreateRequest request) {
    Item targetItem = itemRepository.findFetchItemBorrowerFieldsById(itemId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

    if (!targetItem.isRentalAble()) {
      throw new ApplicationException(ErrorCode.NOT_AVAILABLE_ITEM);
    }

    ItemUnit targetItemUnit = null;
    if (request.itemUnitId() != null) {
      targetItemUnit = itemUnitRepository.findById(request.itemUnitId())
          .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM_UNIT));

      if (!targetItemUnit.getItem().equals(targetItem)) {
        throw new ApplicationException(ErrorCode.ITEM_UNIT_DO_NOT_BELONG_TO_ITEM);
      }
      if (!targetItemUnit.isRentalAble()) {
        throw new ApplicationException(ErrorCode.NOT_AVAILABLE_ITEM_UNIT);
      }
    }

    //itemBorrower Field 를 통해서 request.rentalFields를 확인해야함
    targetItem.validationItemBorrowerFieldsWith(request.renterFields());

    Borrower borrower = Borrower.create(
        request.name(),
        request.phone(),
        objectMapper.valueToTree(request.renterFields())
    );

    Rental requestedRental;
    if (targetItemUnit == null) {
      requestedRental = Rental.request(targetItem.getOrganization(), targetItem, borrower);
    } else {
      requestedRental = Rental.request(targetItem.getOrganization(), targetItem,
          targetItemUnit, borrower);
    }
    rentalRepository.save(requestedRental);

    return new PublicRentalCreateResponse(requestedRental.getId(), targetItem.getId(),
        request.itemUnitId(), requestedRental.getRequestedAt());
  }

  public PublicRentalDetailResponse checkRentalStatusAndDetail(Long rentalId) {
    Rental rental = rentalRepository.findById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    //todo: 장바구니? 기능 이후 name List 를 넘기도록 수정
    String itemName = rental.getRentalItems().getFirst().getItem().getName();
    String itemUnitCode = null;
    if (!rental.getRentalItemUnits().isEmpty()) {
      itemUnitCode = rental.getRentalItemUnits().getFirst().getItemUnit().getCode();
    }

    Map<String, String> borrowerField = objectMapper.convertValue(
        rental.getBorrower().getAdditionalBorrowerInfo(),
        new TypeReference<Map<String, String>>() {}
    );
    return PublicRentalDetailResponse.from(rental, itemName, itemUnitCode, borrowerField);
  }
}
