package retrivr.retrivrspring.presentation.admin.rental.res;

import java.time.LocalDateTime;
import java.util.List;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record AdminRentalRequestPageResponse(
    List<RentalRequestSummary> requests,
    Long nextCursor
) {

  public record RentalRequestSummary(
      Long rentalId,
      Long itemId,
      String itemName,
      Integer rentalDuration,
      Long itemUnitId,
      String itemUnitLabel,
      Integer totalQuantity,
      Integer availableQuantity,
      String borrowerName,
      String contact,
      String guaranteedGoods,
      LocalDateTime requestedAt
  ) {

    public static RentalRequestSummary from(Rental rental) {
      //todo: 장바구니 로직 구현 시 getFirst 삭제
      Item item = rental.getItem();
      ItemUnit itemUnit = null;
      if (rental.hasItemUnit()) {
        itemUnit = rental.getRentalItemUnits().getFirst().getItemUnit();
      }
      Borrower borrower = rental.getBorrower();

      return new RentalRequestSummary(
          rental.getId(),
          item.getId(),
          item.getName(),
          item.getRentalDuration(),
          itemUnit != null? itemUnit.getId() : null,
          itemUnit != null? itemUnit.getLabel() : null,
          item.getTotalQuantity(),
          item.getAvailableQuantity(),
          borrower.getName(),
          borrower.getPhoneNumber(),
          item.getGuaranteedGoods(),
          rental.getRequestedAt()
      );
    }

  }

}
