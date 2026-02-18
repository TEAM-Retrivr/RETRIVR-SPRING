package retrivr.retrivrspring.application.service.item;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.infrastructure.repository.item.ItemRepository;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.presentation.item.res.PublicItemDetailResponse;
import retrivr.retrivrspring.presentation.item.res.PublicItemDetailResponse.PublicItemUnitSummary;
import retrivr.retrivrspring.presentation.item.res.PublicItemListPageResponse;
import retrivr.retrivrspring.presentation.item.res.PublicItemSummary;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemLookupService {

  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;
  private final OrganizationRepository organizationRepository;

  public PublicItemListPageResponse publicOrganizationItemListLookup(Long organizationId,
      Long cursor, int size) {
    boolean isValidOrganization = organizationRepository.existsById(organizationId);
    if (!isValidOrganization) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION);
    }

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(size);

    List<Item> itemList = itemRepository.findPageByOrganizationWithCursor(
        organizationId, cursor, normalizedSize.sizePlusOne());

    boolean hasNext = itemList.size() > normalizedSize.size();
    List<Item> page = hasNext ? itemList.subList(0, normalizedSize.size()) : itemList;

    Long nextCursor = null;
    if (hasNext) {
      nextCursor = page.getLast().getId();
    }

    List<PublicItemSummary> content = page.stream()
        .map(PublicItemSummary::from)
        .toList();

    return new PublicItemListPageResponse(content, nextCursor);
  }

  public PublicItemDetailResponse publicOrganizationItemLookup(Long itemId) {
    if (!itemRepository.existsById(itemId)) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ITEM);
    }

    List<ItemUnit> allByItemId = itemUnitRepository.findAllByItemId(itemId);

    List<PublicItemUnitSummary> list = allByItemId.stream()
        .map(PublicItemUnitSummary::from)
        .toList();

    return new PublicItemDetailResponse(list);
  }
}
