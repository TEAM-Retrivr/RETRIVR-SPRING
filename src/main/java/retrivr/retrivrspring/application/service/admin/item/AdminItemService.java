package retrivr.retrivrspring.application.service.admin.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.item.ItemBorrowerFieldRepository;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.BorrowerRequirementRequest;
import retrivr.retrivrspring.presentation.admin.item.res.*;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

    private final OrganizationRepository organizationRepository;
    private final ItemRepository itemRepository;
    private final ItemBorrowerFieldRepository itemBorrowerFieldRepository;
    private final ItemUnitRepository itemUnitRepository;

    public AdminItemPageResponse getItems(Long organizationId, Long cursor, Integer size) {
        DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
                size);

        List<Item> items = itemRepository.findPageByOrganizationWithCursor(organizationId, cursor,
                normalizedSize.sizePlusOne());

        boolean hasNext = items.size() > normalizedSize.size();
        List<Item> page = hasNext ? items.subList(0, normalizedSize.size()) : items;
        Long nextCursor = hasNext ? page.getLast().getId() : null;

        List<AdminItemListResponse> rows = page.stream()
                .map(AdminItemListResponse::from)
                .toList();

        return new AdminItemPageResponse(rows, nextCursor);
    }

    @Transactional
    public AdminItemCreateResponse createItem(Long organizationId, AdminItemCreateRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        List<BorrowerRequirementRequest> requirements = request.borrowerRequirements();

        Item item = Item.builder()
                .organization(organization)
                .name(request.name())
                .description(request.description())
                .rentalDuration(request.rentalDuration())
                .totalQuantity(request.totalQuantity())
                .availableQuantity(request.totalQuantity())
                .useMessageAlarmService(request.useMessageAlarmService())
                .itemManagementType(request.itemManagementType())
                .guaranteedGoods(request.guaranteedGoods())
                .isActive(true)
                .build();

        Item savedItem = itemRepository.save(item);
        List<ItemBorrowerField> borrowerFields = createBorrowerFields(savedItem, requirements);

        return AdminItemCreateResponse.from(savedItem, borrowerFields);
    }

    @Transactional
    public AdminItemUpdateResponse updateItem(Long organizationId, Long itemId,
                                              AdminItemUpdateRequest request) {

        Item item = itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId,
                        organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

        List<BorrowerRequirementRequest> requirements =
                request.borrowerRequirements();

        item.overwriteAdmin(
                request.name(),
                request.description(),
                request.rentalDuration(),
                request.totalQuantity(),
                request.itemManagementType(),
                request.useMessageAlarmService(),
                request.guaranteedGoods(),
                request.isActive()
        );

        itemBorrowerFieldRepository.deleteByItem(item);
        List<ItemBorrowerField> borrowerFields = createBorrowerFields(item, requirements);

        return AdminItemUpdateResponse.from(item, borrowerFields);
    }


    //이거는 추후 수정 예정
    @Transactional
    public AdminItemUnitMutationResponse updateUnitAvailability(Long organizationId, Long itemId,
                                                                Long itemUnitId, AdminItemUnitAvailabilityUpdateRequest request) {

        Item item = itemRepository.findByIdAndOrganization_Id(itemId, organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

        ItemUnit itemUnit = itemUnitRepository.findByIdAndItemIdAndItemOrganizationId(itemUnitId, itemId,
                        organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM_UNIT));

        boolean wasAvailable = itemUnit.getStatus() == ItemUnitStatus.AVAILABLE;
        itemUnit.changeAvailability(request.isAvailable());

        if (wasAvailable && !request.isAvailable()) {
            item.removeAvailableUnitQuantity();
        } else if (!wasAvailable && request.isAvailable()) {
            item.addAvailableUnitQuantity();
        }

        return AdminItemUnitMutationResponse.from(item, itemUnit);
    }

    private List<ItemBorrowerField> createBorrowerFields(
            Item item,
            List<BorrowerRequirementRequest> requirements
    ) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }

        List<ItemBorrowerField> fields = new ArrayList<>();
        for (int i = 0; i < requirements.size(); i++) {
            BorrowerRequirementRequest req = requirements.get(i);
            fields.add(ItemBorrowerField.of(
                    item,
                    req.label(),
                    req.required(),
                    i + 1
            ));
        }
        return itemBorrowerFieldRepository.saveAll(fields);
    }
}
