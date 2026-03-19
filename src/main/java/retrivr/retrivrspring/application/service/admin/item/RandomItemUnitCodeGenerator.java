package retrivr.retrivrspring.application.service.admin.item;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.service.item.ItemUnitCodeGenerator;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Component
public class RandomItemUnitCodeGenerator implements ItemUnitCodeGenerator {

  private static final char[] CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int CODE_LENGTH = 6;
  private static final int MAX_RETRY = 20;

  private final SecureRandom secureRandom = new SecureRandom();
  private final ItemUnitRepository itemUnitRepository;

  public RandomItemUnitCodeGenerator(ItemUnitRepository itemUnitRepository) {
    this.itemUnitRepository = itemUnitRepository;
  }

  @Override
  public String generate(Item item) {
    for (int i = 0; i < MAX_RETRY; i++) {
      String candidate = randomCode();
      if (!itemUnitRepository.existsByItemIdAndCode(item.getId(), candidate)) {
        return candidate;
      }
    }
    throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION);
  }

  private String randomCode() {
    StringBuilder builder = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      builder.append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)]);
    }
    return builder.toString();
  }
}
