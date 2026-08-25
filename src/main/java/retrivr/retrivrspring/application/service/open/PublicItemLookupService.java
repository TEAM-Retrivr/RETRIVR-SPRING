package retrivr.retrivrspring.application.service.open;

import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.port.image.ImageStoragePort;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipLevel;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemDetailResponse;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemDetailResponse.BorrowerRequirement;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemDetailResponse.PublicItemUnitSummary;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemListPageResponse;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemSummary;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicItemLookupService {

  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;
  private final OrganizationRepository organizationRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final ImageStoragePort imageStoragePort;

  public PublicItemListPageResponse publicOrganizationItemListLookup(Long organizationId,
      Long cursor, int size) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
    organization.assertOperating();

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

    String profileImageUrl = organization.getProfileImageKey() == null
        ? null
        : imageStoragePort.createPresignedDownloadUrl(organization.getProfileImageKey());
    return new PublicItemListPageResponse(
        organizationId,
        organization.getName(),
        profileImageUrl,
        content,
        nextCursor
    );
  }

  public PublicItemDetailResponse publicOrganizationItemLookup(Long itemId) {
    Item item = itemRepository.findFetchItemBorrowerFieldsById(itemId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));
    item.getOrganization().assertOperating();

    List<BorrowerRequirement> borrowerRequirements = item.getItemBorrowerFields().stream()
        .map(BorrowerRequirement::from)
        .toList();
    MembershipLevel membershipLevel = getMembershipLevel(item.getOrganization());
    if (!item.isUnitType()) {
      return new PublicItemDetailResponse(
          List.of(), borrowerRequirements, item.getItemManagementType(), membershipLevel);
    }

    List<ItemUnit> allByItemId = itemUnitRepository.findAllByItemId(itemId);

    List<PublicItemUnitSummary> list = allByItemId.stream()
        .map(PublicItemUnitSummary::from)
        .toList();

    return new PublicItemDetailResponse(
        list, borrowerRequirements, item.getItemManagementType(), membershipLevel);
  }

  private MembershipLevel getMembershipLevel(Organization organization) {
    return membershipPassRepository
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization, MembershipPassStatus.ACTIVE)
        .map(membershipPass -> membershipPass.getLevel())
        .orElse(MembershipLevel.FREE);
  }
}
