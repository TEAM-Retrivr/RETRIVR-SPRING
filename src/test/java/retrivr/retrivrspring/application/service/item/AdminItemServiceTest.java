package retrivr.retrivrspring.application.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.application.service.admin.item.AdminItemService;
import retrivr.retrivrspring.application.service.admin.item.support.AdminItemUnitChangeClassifier;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.item.ItemBorrowerFieldRepository;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.BorrowerRequirementRequest;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemDetailResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemPageResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUnitMutationResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUpdateResponse;

@ExtendWith(MockitoExtension.class)
class AdminItemServiceTest {

  @Mock private OrganizationRepository organizationRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private ItemBorrowerFieldRepository itemBorrowerFieldRepository;
  @Mock private ItemUnitRepository itemUnitRepository;
  @Mock private AdminItemUnitChangeClassifier adminItemUnitChangeClassifier;

  @InjectMocks
  private AdminItemService adminItemService;

  private final AdminItemUnitChangeClassifier realClassifier = new AdminItemUnitChangeClassifier();

  @Test
  @DisplayName("getItems returns desc cursor page")
  void getItems_returnsDescCursorPage() {
    Long organizationId = 1L;
    Item item13 = createItem(13L, "item13", ItemManagementType.UNIT);
    Item item12 = createItem(12L, "item12", ItemManagementType.NON_UNIT);
    Item item11 = createItem(11L, "item11", ItemManagementType.NON_UNIT);
    when(itemRepository.findPageByOrganizationWithCursor(organizationId, 20L, 3))
        .thenReturn(List.of(item13, item12, item11));

    AdminItemPageResponse response = adminItemService.getItems(organizationId, 20L, 2);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.nextCursor()).isEqualTo(12L);
  }

  @Test
  @DisplayName("getItem returns item detail for edit page")
  void getItem_returnsDetail() {
    Long organizationId = 1L;
    Long itemId = 41L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "charger", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    ReflectionTestUtils.setField(item, "rentalDuration", 3);
    ReflectionTestUtils.setField(item, "totalQuantity", 2);
    ReflectionTestUtils.setField(item, "useMessageAlarmService", true);
    ReflectionTestUtils.setField(item, "guaranteedGoods", "student id");
    ReflectionTestUtils.setField(item, "isActive", true);
    item.getItemBorrowerFields().add(
        retrivr.retrivrspring.domain.entity.item.ItemBorrowerField.of(item, "학번", true, 1)
    );

    ItemUnit firstUnit = createItemUnit(1L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ItemUnit secondUnit = createItemUnit(2L, item, "unit-b", ItemUnitStatus.AVAILABLE);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(firstUnit, secondUnit));

    AdminItemDetailResponse response = adminItemService.getItem(organizationId, itemId);

    assertThat(response.itemUnits()).hasSize(2);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-a");
    assertThat(response.borrowerRequirements().get(0).label()).isEqualTo("학번");
  }

  @Test
  @DisplayName("createItem creates unit item with labels")
  void createItem_unitItem_withoutUnits() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> savedUnits = invocation.getArgument(0);
      ReflectionTestUtils.setField(savedUnits.get(0), "id", 101L);
      return savedUnits;
    });

    AdminItemCreateResponse response = adminItemService.createItem(
        organizationId,
        new AdminItemCreateRequest("unit item", "description", 7, 1, ItemManagementType.UNIT, false, null, List.of("기본 충전기"), null)
    );

    assertThat(response.itemId()).isEqualTo(12L);
    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("기본 충전기");
  }

  @Test
  @DisplayName("updateItem renames unit by label")
  void updateItem_renameUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(1, ItemManagementType.UNIT, List.of(unitChange("unit-a", "renamed-unit")));
    stubUnitChangeClassification(List.of(existingUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("renamed-unit");
  }

  @Test
  @DisplayName("updateItem deletes requested unit by label")
  void updateItem_deleteRequestedUnitByLabel() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit firstUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ItemUnit lastUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.AVAILABLE);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(firstUnit, lastUnit));

    AdminItemUpdateRequest request = updateRequest(1, ItemManagementType.UNIT, List.of(unitChange("unit-a", null)));
    stubUnitChangeClassification(List.of(firstUnit, lastUnit), request);

    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(firstUnit, lastUnit), List.of(lastUnit));

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-b");
  }

  @Test
  @DisplayName("updateItem converts non-unit item to unit item with new labels")
  void updateItem_convertNonUnitToUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    List<ItemUnit> savedUnits = new ArrayList<>();

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of()).thenAnswer(invocation -> new ArrayList<>(savedUnits));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> units = invocation.getArgument(0);
      ReflectionTestUtils.setField(units.get(0), "id", 301L);
      ReflectionTestUtils.setField(units.get(1), "id", 302L);
      savedUnits.clear();
      savedUnits.addAll(units);
      return units;
    });

    AdminItemUpdateRequest request = updateRequest(
        2,
        ItemManagementType.UNIT,
        List.of(unitChange(null, "unit-a"), unitChange(null, "unit-b"))
    );
    stubUnitChangeClassification(List.of(), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(2);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-a");
    assertThat(response.itemUnits().get(1).label()).isEqualTo("unit-b");
  }

  @Test
  @DisplayName("updateItem rejects duplicate unit labels in change request")
  void updateItem_rejectsDuplicatedUnitLabel() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange("unit-a", null), unitChange("unit-a", "renamed-unit"))
    );
    when(adminItemUnitChangeClassifier.classify(eq(List.of(existingUnit)), eq(request.unitChanges())))
        .thenThrow(new RuntimeException("Duplicated item unit label in update request."));

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateUnitAvailability changes available unit to inactive")
  void updateUnitAvailability_availableToInactive() {
    Long organizationId = 1L;
    Long itemId = 10L;
    Long itemUnitId = 100L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "unit item", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit itemUnit = createItemUnit(itemUnitId, item, "NB-001", ItemUnitStatus.AVAILABLE);

    when(itemRepository.findByIdAndOrganization_Id(itemId, organizationId)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findByIdAndItemIdAndItemOrganizationId(itemUnitId, itemId, organizationId))
        .thenReturn(Optional.of(itemUnit));

    AdminItemUnitMutationResponse response = adminItemService.updateUnitAvailability(
        organizationId,
        itemId,
        itemUnitId,
        new AdminItemUnitAvailabilityUpdateRequest(false)
    );

    assertThat(response.status()).isEqualTo(ItemUnitStatus.INACTIVE);
    assertThat(response.availableQuantity()).isEqualTo(1);
    assertThat(response.label()).isEqualTo("NB-001");
  }

  private AdminItemUpdateRequest updateRequest(Integer totalQuantity, ItemManagementType type, List<AdminItemUnitChangeRequest> unitChanges) {
    return new AdminItemUpdateRequest(
        "new name",
        "desc",
        5,
        totalQuantity,
        type,
        true,
        "student id",
        unitChanges,
        List.of(new BorrowerRequirementRequest("name", true)),
        true
    );
  }

  private AdminItemUnitChangeRequest unitChange(String currentLabel, String label) {
    return new AdminItemUnitChangeRequest(currentLabel, label);
  }

  private void stubUnitChangeClassification(List<ItemUnit> currentItemUnits, AdminItemUpdateRequest request) {
    when(adminItemUnitChangeClassifier.classify(eq(currentItemUnits), eq(request.unitChanges())))
        .thenReturn(realClassifier.classify(currentItemUnits, request.unitChanges()));
  }

  private Organization createOrganization(Long id) {
    return Organization.builder()
        .id(id)
        .email("org" + id + "@example.com")
        .passwordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M6Qp6xGX2YeliYg5OtTSGTN/xGHy")
        .name("org" + id)
        .status(OrganizationStatus.ACTIVE)
        .searchKey("org-" + id)
        .adminCodeHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M6Qp6xGX2YeliYg5OtTSGTN/xGHy")
        .build();
  }

  private Item createItem(Long id, String name, ItemManagementType itemManagementType) {
    Item item = Item.builder()
        .name(name)
        .rentalDuration(3)
        .description("desc")
        .useMessageAlarmService(false)
        .totalQuantity(0)
        .availableQuantity(0)
        .isActive(true)
        .itemManagementType(itemManagementType)
        .itemBorrowerFields(new ArrayList<>())
        .build();
    ReflectionTestUtils.setField(item, "id", id);
    return item;
  }

  private ItemUnit createItemUnit(Long id, Item item, String label, ItemUnitStatus status) {
    ItemUnit itemUnit = ItemUnit.builder()
        .item(item)
        .label(label)
        .status(status)
        .build();
    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }

  private void setQuantities(Item item, int totalQuantity, int availableQuantity) {
    ReflectionTestUtils.setField(item, "totalQuantity", totalQuantity);
    ReflectionTestUtils.setField(item, "availableQuantity", availableQuantity);
  }
}
