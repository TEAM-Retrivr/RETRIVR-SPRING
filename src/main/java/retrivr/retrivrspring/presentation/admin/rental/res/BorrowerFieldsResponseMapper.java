package retrivr.retrivrspring.presentation.admin.rental.res;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import retrivr.retrivrspring.domain.entity.rental.Borrower;

final class BorrowerFieldsResponseMapper {

  private BorrowerFieldsResponseMapper() {
  }

  static Map<String, String> from(Borrower borrower) {
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
