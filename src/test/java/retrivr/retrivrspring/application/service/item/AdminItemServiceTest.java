package retrivr.retrivrspring.application.service.item;

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
import retrivr.retrivrspring.domain.service.item.ItemUnitCodeGenerator;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.BorrowerRequirementRequest;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemPageResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUnitMutationResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUpdateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminItemServiceTest {

  @Mock
  private OrganizationRepository organizationRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private ItemBorrowerFieldRepository itemBorrowerFieldRepository;

  @Mock
  private ItemUnitRepository itemUnitRepository;

  @Mock
  private ItemUnitCodeGenerator itemUnitCodeGenerator;

  @Mock
  private AdminItemUnitChangeClassifier adminItemUnitChangeClassifier;

  @InjectMocks
  private AdminItemService adminItemService;

  private final AdminItemUnitChangeClassifier realAdminItemUnitChangeClassifier =
      new AdminItemUnitChangeClassifier();

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
  @DisplayName("createItem creates unit item with generated unit code")
  void createItem_unitItem_withoutUnits() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });
    when(itemUnitCodeGenerator.generate(any(Item.class))).thenReturn("AB12CD");
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> savedUnits = invocation.getArgument(0);
      ReflectionTestUtils.setField(savedUnits.get(0), "id", 101L);
      return savedUnits;
    });

    AdminItemCreateRequest request = new AdminItemCreateRequest(
        "unit item",
        "description",
        7,
        1,
        ItemManagementType.UNIT,
        false,
        null,
        List.of("기본 충전기"),
        null
    );

    AdminItemCreateResponse response = adminItemService.createItem(organizationId, request);

    assertThat(response.itemId()).isEqualTo(12L);
    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("기본 충전기");
    assertThat(response.itemUnits().get(0).code()).isEqualTo("AB12CD");
    assertThat(response.borrowerRequirements()).isEmpty();
  }

  @Test
  @DisplayName("createItem returns borrower requirements from request")
  void createItem_withBorrowerRequirements() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemCreateRequest request = new AdminItemCreateRequest(
        "charger",
        "desc",
        3,
        1,
        ItemManagementType.NON_UNIT,
        false,
        null,
        null,
        List.of(new BorrowerRequirementRequest("name", true))
    );

    AdminItemCreateResponse response = adminItemService.createItem(organizationId, request);

    assertThat(response.borrowerRequirements()).hasSize(1);
    assertThat(response.itemUnits()).isEmpty();
    assertThat(response.borrowerRequirements().get(0).label()).isEqualTo("name");
  }

  @Test
  @DisplayName("updateItem updates admin fields")
  void updateItem_updatesFields() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(existingUnit, "code", "B2C3D4");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        null
    );
    stubUnitChangeClassification(List.of(existingUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.name()).isEqualTo("new name");
    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.useMessageAlarmService()).isTrue();
    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-a");
  }

  @Test
  @DisplayName("updateItem deletes only tail units")
  void updateItem_deleteOnlyFromTail() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);

    ItemUnit firstUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(firstUnit, "code", "A1B2C3");
    ItemUnit lastUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(lastUnit, "code", "B2C3D4");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(firstUnit, lastUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange("A1B2C3", null))
    );
    stubUnitChangeClassification(List.of(firstUnit, lastUnit), request);

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateItem deletes and adds unit with new code")
  void updateItem_deleteAndAddUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);

    ItemUnit oldUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(oldUnit, "code", "A1B2C3");
    ItemUnit newUnit = createItemUnit(301L, item, "unit-new", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(newUnit, "code", "N9X8Y7");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(oldUnit), List.of(newUnit));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitCodeGenerator.generate(any(Item.class))).thenReturn("N9X8Y7");
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> savedUnits = invocation.getArgument(0);
      ReflectionTestUtils.setField(savedUnits.get(0), "id", 301L);
      return savedUnits;
    });

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(
            unitChange("A1B2C3", null),
            unitChange(null, "unit-new")
        )
    );
    stubUnitChangeClassification(List.of(oldUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-new");
    assertThat(response.itemUnits().get(0).code()).isEqualTo("N9X8Y7");
  }

  @Test
  @DisplayName("updateItem renames unit by code")
  void updateItem_renameUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);

    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(existingUnit, "code", "A1B2C3");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange("A1B2C3", "renamed-unit"))
    );
    stubUnitChangeClassification(List.of(existingUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).code()).isEqualTo("A1B2C3");
    assertThat(response.itemUnits().get(0).label()).isEqualTo("renamed-unit");
  }

  @Test
  @DisplayName("updateItem rejects deleting rented or pending units")
  void updateItem_deleteRentedOrPendingUnit_throws() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 1);

    ItemUnit availableUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(availableUnit, "code", "A1B2C3");
    ItemUnit pendingUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.RENTAL_PENDING);
    ReflectionTestUtils.setField(pendingUnit, "code", "B2C3D4");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(availableUnit, pendingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange("B2C3D4", null))
    );
    stubUnitChangeClassification(List.of(availableUnit, pendingUnit), request);

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateItem rejects inconsistent total quantity for unit changes")
  void updateItem_rejectsMismatchedTotalQuantity() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);

    ItemUnit firstUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(firstUnit, "code", "A1B2C3");
    ItemUnit secondUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(secondUnit, "code", "B2C3D4");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(firstUnit, secondUnit));

    AdminItemUpdateRequest request = updateRequest(
        2,
        ItemManagementType.UNIT,
        List.of(unitChange("B2C3D4", null))
    );
    stubUnitChangeClassification(List.of(firstUnit, secondUnit), request);

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateItem converts non-unit item to unit item with new unit codes")
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
    when(itemUnitRepository.findAllByItemId(itemId))
        .thenReturn(List.of())
        .thenAnswer(invocation -> new ArrayList<>(savedUnits));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitCodeGenerator.generate(any(Item.class))).thenReturn("NEW001", "NEW002");
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
        List.of(
            unitChange(null, "unit-a"),
            unitChange(null, "unit-b")
        )
    );
    stubUnitChangeClassification(List.of(), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.itemUnits()).hasSize(2);
    assertThat(response.itemUnits().get(0).code()).isEqualTo("NEW001");
    assertThat(response.itemUnits().get(1).code()).isEqualTo("NEW002");
    assertThat(item.getAvailableQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("updateItem converts unit item to non-unit item after deleting all units")
  void updateItem_convertUnitToNonUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 1);

    ItemUnit availableUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(availableUnit, "code", "A1B2C3");
    ItemUnit inactiveUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.INACTIVE);
    ReflectionTestUtils.setField(inactiveUnit, "code", "B2C3D4");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(availableUnit, inactiveUnit), List.of());
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(
        3,
        ItemManagementType.NON_UNIT,
        List.of(
            unitChange("A1B2C3", null),
            unitChange("B2C3D4", null)
        )
    );
    stubUnitChangeClassification(List.of(availableUnit, inactiveUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.NON_UNIT);
    assertThat(response.itemUnits()).isEmpty();
    assertThat(item.getAvailableQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("updateItem rejects non-unit to unit conversion when stock is unavailable")
  void updateItem_convertNonUnitToUnitWithUnavailableQuantity_throws() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 1);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of());

    AdminItemUpdateRequest request = updateRequest(
        2,
        ItemManagementType.UNIT,
        List.of(
            unitChange(null, "unit-a"),
            unitChange(null, "unit-b")
        )
    );
    stubUnitChangeClassification(List.of(), request);

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateItem rejects unit creation for non-unit target")
  void updateItem_rejectsUnitCreationForNonUnitTarget() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of());

    AdminItemUpdateRequest request = updateRequest(
        2,
        ItemManagementType.NON_UNIT,
        List.of(unitChange(null, "unit-a"))
    );
    stubUnitChangeClassification(List.of(), request);

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("updateItem rejects duplicate unit codes in change request")
  void updateItem_rejectsDuplicatedUnitCode() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);

    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ReflectionTestUtils.setField(existingUnit, "code", "A1B2C3");

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(
            unitChange("A1B2C3", null),
            unitChange("A1B2C3", "renamed-unit")
        )
    );
    when(adminItemUnitChangeClassifier.classify(eq(List.of(existingUnit)), eq(request.unitChanges())))
        .thenThrow(new RuntimeException("Duplicated item unit code in update request."));

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
  }

  @Test
  @DisplayName("updateUnitAvailability rejects rented unit transition")
  void updateUnitAvailability_rentedUnit_throws() {
    Long organizationId = 1L;
    Long itemId = 10L;
    Long itemUnitId = 100L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "unit item", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    ItemUnit itemUnit = createItemUnit(itemUnitId, item, "NB-001", ItemUnitStatus.RENTED);

    when(itemRepository.findByIdAndOrganization_Id(itemId, organizationId)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findByIdAndItemIdAndItemOrganizationId(itemUnitId, itemId, organizationId))
        .thenReturn(Optional.of(itemUnit));

    assertThatThrownBy(() -> adminItemService.updateUnitAvailability(
        organizationId,
        itemId,
        itemUnitId,
        new AdminItemUnitAvailabilityUpdateRequest(false)
    )).isInstanceOf(RuntimeException.class);
  }

  private AdminItemUpdateRequest updateRequest(
      Integer totalQuantity,
      ItemManagementType itemManagementType,
      List<AdminItemUnitChangeRequest> unitChanges
  ) {
    return new AdminItemUpdateRequest(
        "new name",
        "desc",
        5,
        totalQuantity,
        itemManagementType,
        true,
        "student id",
        unitChanges,
        List.of(new BorrowerRequirementRequest("name", true)),
        true
    );
  }

  private AdminItemUnitChangeRequest unitChange(String code, String label) {
    return new AdminItemUnitChangeRequest(code, label);
  }

  private void stubUnitChangeClassification(List<ItemUnit> currentItemUnits, AdminItemUpdateRequest request) {
    when(adminItemUnitChangeClassifier.classify(eq(currentItemUnits), eq(request.unitChanges())))
        .thenReturn(realAdminItemUnitChangeClassifier.classify(currentItemUnits, request.unitChanges()));
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
        .guaranteedGoods(null)
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
        .code(null)
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
