package retrivr.retrivrspring.application.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

public record OrganizationSearchCursor(
    int bucket,
    double sim,
    long organizationId
) {

  private static final ObjectMapper om = new ObjectMapper();

  public String encode() {
    try {
      String json = om.writeValueAsString(this);
      return Base64.getUrlEncoder().withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new ApplicationException(ErrorCode.DO_NOT_ENCODED_SEARCH_CURSOR);
    }
  }

  public static OrganizationSearchCursor decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(cursor);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return om.readValue(json, OrganizationSearchCursor.class);
    } catch (Exception e) {
      throw new ApplicationException(ErrorCode.INVALID_SEARCH_CURSOR);
    }
  }
}
