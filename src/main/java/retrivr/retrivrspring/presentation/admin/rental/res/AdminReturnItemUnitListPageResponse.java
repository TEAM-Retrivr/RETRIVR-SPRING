package retrivr.retrivrspring.presentation.admin.rental.res;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
      String borrowerPhone,
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
          borrower.getPhone() != null ? borrower.getPhone().getPhone() : null,
          extractBorrowerFields(borrower),
          rental.getRequestNote(),
          rental.getDecidedAt().toLocalDate(),
          rental.getDueDate()
      );
    }

    private static Map<String, String> extractBorrowerFields(Borrower borrower) {
      Map<String, String> fields = new LinkedHashMap<>();
      JsonNode additionalBorrowerInfo = borrower.getAdditionalBorrowerInfo();
      if (additionalBorrowerInfo == null || additionalBorrowerInfo.isNull()) {
        return fields;
      }

      additionalBorrowerInfo.fields()
          .forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue().asText("")));
      return fields;
    }

  }

}
