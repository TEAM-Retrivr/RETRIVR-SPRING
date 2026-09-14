package retrivr.retrivrspring.presentation.admin.item.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.item.Item;

public record AdminItemDeleteResponse(Long itemId, LocalDateTime deletedAt) {
  public static AdminItemDeleteResponse from(Item item) {
    return new AdminItemDeleteResponse(item.getId(), item.getDeletedAt());
  }
}
