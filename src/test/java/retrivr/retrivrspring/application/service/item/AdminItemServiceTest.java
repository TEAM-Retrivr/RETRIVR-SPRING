package retrivr.retrivrspring.application.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import retrivr.retrivrspring.application.port.id.PublicIdGenerator;
import retrivr.retrivrspring.application.service.admin.auth.AdminCodeVerificationService;
import retrivr.retrivrspring.application.service.admin.item.AdminItemService;
import retrivr.retrivrspring.application.service.admin.item.support.AdminItemUnitChangeClassifier;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.item.ItemBorrowerFieldRepository;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemActivationUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitAvailabilityUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.req.BorrowerRequirementRequest;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemActivationUpdateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemDeleteResponse;
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
  @Mock private RentalRepository rentalRepository;
  @Mock private AdminItemUnitChangeClassifier adminItemUnitChangeClassifier;
  @Mock private PublicIdGenerator publicIdGenerator;
  @Mock private AdminCodeVerificationService adminCodeVerificationService;

  @InjectMocks
  private AdminItemService adminItemService;

  private final AdminItemUnitChangeClassifier realClassifier = new AdminItemUnitChangeClassifier();

  @Test
  void updateActivation_changesOnlyActivationState() {
    Item item = createItem(1L, "charger", ItemManagementType.NON_UNIT);
    when(itemRepository.findByIdAndOrganization_Id(1L, 1L)).thenReturn(Optional.of(item));

    AdminItemActivationUpdateResponse response = adminItemService.updateActivation(
        1L, 1L, new AdminItemActivationUpdateRequest(false));

    assertThat(response.isActive()).isFalse();
  }

  @Test
  void deleteItem_recordsSoftDeletionWhenNoActiveRentalExists() {
    Item item = createItem(1L, "charger", ItemManagementType.NON_UNIT);
    when(itemRepository.findByIdAndOrganization_Id(1L, 1L)).thenReturn(Optional.of(item));

    AdminItemDeleteResponse response = adminItemService.deleteItem(1L, 1L);

    assertThat(response.itemId()).isEqualTo(1L);
    assertThat(response.deletedAt()).isNotNull();
    assertThat(item.isActive()).isFalse();
  }

  @Test
  void deleteItem_rejectsWhenRequestedOrRentedRentalExists() {
    Item item = createItem(1L, "charger", ItemManagementType.NON_UNIT);
    when(itemRepository.findByIdAndOrganization_Id(1L, 1L)).thenReturn(Optional.of(item));
    when(rentalRepository.existsByRentalItems_Item_IdAndStatusIn(
        1L, List.of(RentalStatus.REQUESTED, RentalStatus.RENTED))).thenReturn(true);

    assertThatThrownBy(() -> adminItemService.deleteItem(1L, 1L))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ITEM_DELETE_WITH_ACTIVE_RENTAL);
  }

  @Test
  @DisplayName("물품 목록을 내림차순 커서 페이지로 조회한다")
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
  @DisplayName("수정 화면에 필요한 물품 상세 정보를 조회한다")
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
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(firstUnit, secondUnit));

    AdminItemDetailResponse response = adminItemService.getItem(organizationId, itemId);

    assertThat(response.itemUnits()).hasSize(2);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-a");
    assertThat(response.itemUnits().get(0).status()).isEqualTo(ItemUnitStatus.AVAILABLE);
    assertThat(response.borrowerRequirements().get(0).label()).isEqualTo("학번");
  }

  @Test
  @DisplayName("UNIT 물품 생성 시 유닛 label을 함께 생성한다")
  void createItem_unitItem_withoutUnits() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.saveAndFlush(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> savedUnits = invocation.getArgument(0);
      ReflectionTestUtils.setField(savedUnits.get(0), "id", 101L);
      return savedUnits;
    });
    when(publicIdGenerator.generateItemId(any())).thenAnswer(invocation -> "public-id");

    AdminItemCreateResponse response = adminItemService.createItem(
        organizationId,
        new AdminItemCreateRequest("unit item", "description", 7, 1, ItemManagementType.UNIT, false, null, List.of("기본 충전기"), null)
    );

    assertThat(response.itemId()).isEqualTo(12L);
    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("기본 충전기");
  }

  @Test
  @DisplayName("앞뒤 공백을 제거한 유닛 이름이 중복되면 물품을 생성할 수 없다")
  void createItem_rejectsDuplicatedTrimmedUnitLabels() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.saveAndFlush(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });
    when(publicIdGenerator.generateItemId(any())).thenReturn("public-id");

    assertThatThrownBy(() -> adminItemService.createItem(
        organizationId,
        new AdminItemCreateRequest("unit item", "description", 7, 2,
            ItemManagementType.UNIT, false, null,
            List.of(" same-label ", "same-label"), null)
    ))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DUPLICATE_ITEM_UNIT_LABEL);
  }

  @Test
  @DisplayName("itemUnitId로 유닛 이름을 변경한다")
  void updateItem_renameUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(1, ItemManagementType.UNIT, List.of(unitChange(201L, "renamed-unit")));
    stubUnitChangeClassification(List.of(existingUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("renamed-unit");
  }

  @Test
  @DisplayName("활성 유닛의 label이 같아도 요청한 ID의 유닛만 이름을 변경한다")
  void updateItem_renamesOnlyRequestedUnitWhenLabelsAreSame() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit firstUnit = createItemUnit(201L, item, "same-label", ItemUnitStatus.AVAILABLE);
    ItemUnit secondUnit = createItemUnit(202L, item, "same-label", ItemUnitStatus.AVAILABLE);
    List<ItemUnit> activeUnits = List.of(firstUnit, secondUnit);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId)).thenReturn(activeUnits);
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(
        2, ItemManagementType.UNIT, List.of(unitChange(201L, "renamed-unit")));
    stubUnitChangeClassification(activeUnits, request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).extracting("itemUnitId", "label")
        .containsExactly(
            tuple(201L, "renamed-unit"),
            tuple(202L, "same-label")
        );
  }

  @Test
  @DisplayName("활성 유닛의 label이 같아도 요청한 ID의 유닛만 삭제한다")
  void updateItem_deletesOnlyRequestedUnitWhenLabelsAreSame() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit firstUnit = createItemUnit(201L, item, "same-label", ItemUnitStatus.AVAILABLE);
    ItemUnit secondUnit = createItemUnit(202L, item, "same-label", ItemUnitStatus.AVAILABLE);
    List<ItemUnit> activeUnits = List.of(firstUnit, secondUnit);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(activeUnits)
        .thenReturn(List.of(secondUnit));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(
        1, ItemManagementType.UNIT, List.of(unitChange(201L, null)));
    stubUnitChangeClassification(activeUnits, request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).extracting("itemUnitId", "label")
        .containsExactly(tuple(202L, "same-label"));
    verify(itemUnitRepository).deleteAll(List.of(firstUnit));
    assertThat(item.getTotalQuantity()).isEqualTo(1);
    assertThat(item.getAvailableQuantity()).isEqualTo(1);
  }

  @Test
  @DisplayName("유닛 삭제와 동일 label 재생성을 한 요청에서 처리하고 새 ID를 발급한다")
  void updateItem_deletesAndRecreatesSameLabelWithNewId() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "same-label", ItemUnitStatus.AVAILABLE);
    List<ItemUnit> createdUnits = new ArrayList<>();

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(existingUnit))
        .thenAnswer(invocation -> new ArrayList<>(createdUnits));
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> units = invocation.getArgument(0);
      ReflectionTestUtils.setField(units.getFirst(), "id", 301L);
      createdUnits.addAll(units);
      return units;
    });
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange(201L, null), unitChange(null, "same-label"))
    );
    stubUnitChangeClassification(List.of(existingUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).extracting("itemUnitId", "label")
        .containsExactly(tuple(301L, "same-label"));
    verify(itemUnitRepository).deleteAll(List.of(existingUnit));
    assertThat(item.getTotalQuantity()).isEqualTo(1);
    assertThat(item.getAvailableQuantity()).isEqualTo(1);
  }

  @Test
  @DisplayName("대여 이력이 있는 유닛을 논리 삭제한다")
  void updateItem_logicallyDeletesUnitWithRentalHistory() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit firstUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ItemUnit lastUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(firstUnit, lastUnit));
    when(rentalRepository.existsByRentalItemUnits_ItemUnit_Id(201L)).thenReturn(true);

    AdminItemUpdateRequest request = updateRequest(1, ItemManagementType.UNIT, List.of(unitChange(201L, null)));
    stubUnitChangeClassification(List.of(firstUnit, lastUnit), request);

    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId)).thenReturn(
        List.of(firstUnit, lastUnit), List.of(lastUnit));

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().get(0).label()).isEqualTo("unit-b");
    assertThat(firstUnit.isDeleted()).isTrue();
  }

  @Test
  void updateItem_allowsReuseOfLogicallyDeletedUnitLabel() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 0);
    ItemUnit deletedUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    deletedUnit.delete();
    List<ItemUnit> createdUnits = new ArrayList<>();

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of())
        .thenAnswer(invocation -> new ArrayList<>(createdUnits));
    when(itemUnitRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItemUnit> units = invocation.getArgument(0);
      ReflectionTestUtils.setField(units.getFirst(), "id", 301L);
      createdUnits.addAll(units);
      return units;
    });
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(
        1, ItemManagementType.UNIT, List.of(unitChange(null, "unit-a")));
    stubUnitChangeClassification(List.of(), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemUnits()).hasSize(1);
    assertThat(response.itemUnits().getFirst().itemUnitId()).isEqualTo(301L);
    assertThat(response.itemUnits().getFirst().label()).isEqualTo("unit-a");
    assertThat(deletedUnit.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("NON_UNIT 물품을 새 유닛과 함께 UNIT 물품으로 전환한다")
  void updateItem_convertNonUnitToUnit() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    List<ItemUnit> savedUnits = new ArrayList<>();

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of())
        .thenAnswer(invocation -> new ArrayList<>(savedUnits));
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
  @DisplayName("대여 이력이 없는 유닛을 물리 삭제하며 UNIT 물품을 NON_UNIT으로 전환한다")
  void updateItem_convertUnitToNonUnit_physicallyDeletesUnusedUnits() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 2, 2);
    ItemUnit firstUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);
    ItemUnit secondUnit = createItemUnit(202L, item, "unit-b", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(firstUnit, secondUnit))
        .thenReturn(List.of());
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = updateRequest(2, ItemManagementType.NON_UNIT, List.of());
    stubUnitChangeClassification(List.of(firstUnit, secondUnit), request);

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.NON_UNIT);
    assertThat(response.itemUnits()).isEmpty();
    verify(itemUnitRepository).deleteAll(List.of(firstUnit, secondUnit));
  }

  @Test
  @DisplayName("변경 요청에 동일한 itemUnitId가 중복되면 거부한다")
  void updateItem_rejectsDuplicatedItemUnitId() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    setQuantities(item, 1, 1);
    ItemUnit existingUnit = createItemUnit(201L, item, "unit-a", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganizationIdForUpdate(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemIdAndDeletedAtIsNull(itemId))
        .thenReturn(List.of(existingUnit));

    AdminItemUpdateRequest request = updateRequest(
        1,
        ItemManagementType.UNIT,
        List.of(unitChange(201L, null), unitChange(201L, "renamed-unit"))
    );
    when(adminItemUnitChangeClassifier.classify(eq(List.of(existingUnit)), eq(request.unitChanges())))
        .thenThrow(new RuntimeException("Duplicated itemUnitId in update request."));

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("대여 가능한 유닛을 비활성 상태로 변경한다")
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
    when(itemUnitRepository.findByIdAndItemIdAndItemOrganizationIdAndDeletedAtIsNull(
        itemUnitId, itemId, organizationId))
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
        true,
        "token"
    );
  }

  private AdminItemUnitChangeRequest unitChange(Long itemUnitId, String label) {
    return new AdminItemUnitChangeRequest(itemUnitId, label);
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
