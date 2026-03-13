package retrivr.retrivrspring.application.service.admin.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.enumerate.BorrowerFieldType;
import retrivr.retrivrspring.domain.repository.item.ItemBorrowerFieldRepository;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.res.*;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminItemService {

  private static final int DEFAULT_TOTAL_QUANTITY = 0;
  private static final int DEFAULT_AVAILABLE_QUANTITY = 0;
  private static final boolean DEFAULT_USE_MESSAGE_ALARM_SERVICE = false;
  private static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("custom_[1-9]\\d*");
  private static final Map<String, String> PRESET_FIELD_LABELS = Map.of(
      "name", "이름",
      "studentNumber", "학번",
      "phone", "전화번호"
  );

  private final OrganizationRepository organizationRepository;
  private final ItemRepository itemRepository;
  private final ItemBorrowerFieldRepository itemBorrowerFieldRepository;
  private final ItemUnitRepository itemUnitRepository;

  public AdminItemPageResponse getItems(Long organizationId, Long cursor, Integer size) {
    validateOrganizationExists(organizationId);

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        size);

    List<Item> items = itemRepository.findPageByOrganizationWithCursor(organizationId, cursor,
        normalizedSize.sizePlusOne());

    boolean hasNext = items.size() > normalizedSize.size();
    List<Item> page = hasNext ? items.subList(0, normalizedSize.size()) : items;
    Long nextCursor = hasNext ? page.getLast().getId() : null;

    List<AdminItemListResponse> rows = page.stream()
        .map(item -> {
          List<ItemUnit> itemUnits = item.isUnitType()
              ? itemUnitRepository.findAllByItemId(item.getId())
              : List.of();
          return AdminItemListResponse.from(item, itemUnits);
        })
        .toList();

    return new AdminItemPageResponse(rows, nextCursor);
  }

  @Transactional
  public AdminItemCreateResponse createItem(Long organizationId, AdminItemCreateRequest request) {
    Organization organization = getOrganization(organizationId);
    List<AdminItemCreateRequest.BorrowerRequirement> requirements =
        resolveBorrowerRequirements(request.borrowerRequirements());

    Item item = Item.builder()
        .organization(organization)
        .name(request.name())
        .description(request.description())
        .rentalDuration(request.rentalDuration())
        .guaranteedGoods(null)
        .useMessageAlarmService(DEFAULT_USE_MESSAGE_ALARM_SERVICE)
        .totalQuantity(DEFAULT_TOTAL_QUANTITY)
        .availableQuantity(DEFAULT_AVAILABLE_QUANTITY)
        .isActive(request.isActive())
        .itemManagementType(request.itemManagementType())
        .build();

    Item savedItem = itemRepository.save(item);
    List<ItemBorrowerField> borrowerFields = createBorrowerFields(savedItem, requirements);

    return AdminItemCreateResponse.from(savedItem, borrowerFields);
  }

  @Transactional
  public AdminItemUpdateResponse updateItem(Long organizationId, Long itemId,
      AdminItemUpdateRequest request) {
    getOrganization(organizationId);
    List<AdminItemCreateRequest.BorrowerRequirement> requirements =
        resolveBorrowerRequirements(request.borrowerRequirements());

    Item item = itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId,
            organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

    validateManagementTypeTransition(item, request.itemManagementType());

    item.overwriteAdmin(
        request.name(),
        request.description(),
        request.rentalDuration(),
        request.isActive(),
        request.itemManagementType(),
        item.getTotalQuantity(),
        item.getAvailableQuantity()
    );

    itemBorrowerFieldRepository.deleteByItem(item);
    List<ItemBorrowerField> borrowerFields = createBorrowerFields(item, requirements);

    return AdminItemUpdateResponse.from(item, borrowerFields);
  }

  @Transactional
  public AdminItemUnitMutationResponse updateUnitAvailability(Long organizationId, Long itemId,
      Long itemUnitId, AdminItemUnitAvailabilityUpdateRequest request) {
    getOrganization(organizationId);

    Item item = itemRepository.findByIdAndOrganization_Id(itemId, organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));
    validateUnitType(item);

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

  private List<ItemBorrowerField> createBorrowerFields(Item item,
      List<AdminItemCreateRequest.BorrowerRequirement> requirements) {
    if (requirements.isEmpty()) {
      return List.of();
    }

    List<ItemBorrowerField> borrowerFields = new ArrayList<>();
    for (int index = 0; index < requirements.size(); index++) {
      AdminItemCreateRequest.BorrowerRequirement requirement = requirements.get(index);
      borrowerFields.add(ItemBorrowerField.builder()
          .item(item)
          .fieldKey(requirement.fieldKey())
          .label(requirement.label())
          .fieldType(requirement.fieldType())
          .isRequired(requirement.required())
          .sortOrder(index + 1)
          .build());
    }

    return itemBorrowerFieldRepository.saveAll(borrowerFields);
  }

  private List<AdminItemCreateRequest.BorrowerRequirement> resolveBorrowerRequirements(
      List<AdminItemCreateRequest.BorrowerRequirement> borrowerRequirements) {
    List<AdminItemCreateRequest.BorrowerRequirement> normalizedRequirements =
        borrowerRequirements == null ? defaultBorrowerRequirements() : borrowerRequirements;

    validateBorrowerRequirements(normalizedRequirements);
    return normalizedRequirements;
  }

  private void validateBorrowerRequirements(
      List<AdminItemCreateRequest.BorrowerRequirement> borrowerRequirements) {
    Set<String> fieldKeys = new HashSet<>();

    for (AdminItemCreateRequest.BorrowerRequirement borrowerRequirement : borrowerRequirements) {
      String fieldKey = borrowerRequirement.fieldKey();

      if (!fieldKeys.add(fieldKey)) {
        throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
            "Duplicate borrower requirement fieldKey: " + fieldKey);
      }

      if (isPresetField(fieldKey)) {
        validatePresetFieldLabel(fieldKey, borrowerRequirement.label());
        continue;
      }

      if (!CUSTOM_FIELD_KEY_PATTERN.matcher(fieldKey).matches()) {
        throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
            "Invalid borrower requirement fieldKey: " + fieldKey);
      }
    }
  }

  private void validateManagementTypeTransition(Item item, ItemManagementType requestedType) {
    if (item.getItemManagementType() != requestedType) {
      throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
          "Changing itemManagementType is not allowed");
    }
  }

  private void validateUnitType(Item item) {
    if (!item.isUnitType()) {
      throw new ApplicationException(ErrorCode.ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE);
    }
  }

  private boolean isPresetField(String fieldKey) {
    return PRESET_FIELD_LABELS.containsKey(fieldKey);
  }

  private void validatePresetFieldLabel(String fieldKey, String label) {
    String expectedLabel = PRESET_FIELD_LABELS.get(fieldKey);
    if (!expectedLabel.equals(label)) {
      throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
          "Invalid label for preset fieldKey: " + fieldKey);
    }
  }

  private List<AdminItemCreateRequest.BorrowerRequirement> defaultBorrowerRequirements() {
    return List.of(
        new AdminItemCreateRequest.BorrowerRequirement("name", "이름", BorrowerFieldType.TEXT,
            true),
        new AdminItemCreateRequest.BorrowerRequirement("studentNumber", "학번",
            BorrowerFieldType.TEXT, true),
        new AdminItemCreateRequest.BorrowerRequirement("phone", "전화번호",
            BorrowerFieldType.PHONE, true)
    );
  }

  private Organization getOrganization(Long organizationId) {
    return organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  private void validateOrganizationExists(Long organizationId) {
    if (!organizationRepository.existsById(organizationId)) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION);
    }
  }
}
