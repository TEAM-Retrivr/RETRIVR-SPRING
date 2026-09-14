package retrivr.retrivrspring.presentation.admin.item.res;

import retrivr.retrivrspring.domain.entity.item.Item;

public record AdminItemActivationUpdateResponse(Long itemId, Boolean isActive) {
  public static AdminItemActivationUpdateResponse from(Item item) {
    return new AdminItemActivationUpdateResponse(item.getId(), item.isActive());
  }
}
