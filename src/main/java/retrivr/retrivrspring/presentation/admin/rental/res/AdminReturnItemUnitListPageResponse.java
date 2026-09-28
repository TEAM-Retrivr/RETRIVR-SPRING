package retrivr.retrivrspring.presentation.admin.rental.res;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;

public record AdminReturnItemUnitListPageResponse(
    Long itemId,
    String itemName,
    String guaranteedGoods,
    Integer availableQuantity,
    Integer totalQuantity,
    Integer rentalDuration,
    List<BorrowedItemSummary> borrowedItems,
    Long nextCursor
) {

  public record BorrowedItemSummary(
      Long rentalId,
      boolean isOverdue,
      Long unitId,
      String borrowedItemName,
      String itemUnitLabel,
      String borrowerName,
      String contact,
      Map<String, String> borrowerFields,
      String requestNote,
      LocalDate rentalDate,
      LocalDate expectedReturnDueDate
  ) {

    public static BorrowedItemSummary fromUnit(ItemUnit itemUnit, Rental rental) {
      return from(rental, itemUnit.getId(), itemUnit.getLabel());
    }

    public static BorrowedItemSummary fromNonUnit(Item item, Rental rental) {
      return from(rental, null, item.getName());
    }

    private static BorrowedItemSummary from(Rental rental, Long unitId, String borrowedItemName) {
      Borrower borrower = rental.getBorrower();
      return new BorrowedItemSummary(
          rental.getId(),
          rental.isOverdue(),
          unitId,
          borrowedItemName,
          unitId != null ? borrowedItemName : null,
          borrower.getName(),
          borrower.getContact(),
          BorrowerFieldsResponseMapper.from(borrower),
          rental.getRequestNote(),
          rental.getDecidedAt().toLocalDate(),
          rental.getDueDate()
      );
    }

  }

}
