package retrivr.retrivrspring.application.service.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import retrivr.retrivrspring.application.service.admin.item.AdminItemService;
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

  @InjectMocks
  private AdminItemService adminItemService;

  @Test
  @DisplayName("getItems는 DESC cursor pagination을 반환한다")
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
  @DisplayName("createItem은 UNIT 물품도 유닛 없이 생성한다")
  void createItem_unitItem_withoutUnits() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      Item saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 12L);
      return saved;
    });

    AdminItemCreateRequest request = new AdminItemCreateRequest(
        "unit item",
        "description",
        7,
        1,
        ItemManagementType.UNIT,
        false,
        null,
        null,
        null
    );

    AdminItemCreateResponse response = adminItemService.createItem(organizationId, request);

    assertThat(response.itemId()).isEqualTo(12L);
    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
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
    assertThat(response.borrowerRequirements().get(0).label()).isEqualTo("name");
  }

  @Test
  @DisplayName("updateItem updates admin fields")
  void updateItem_updatesFields() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);

    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemUpdateRequest request = new AdminItemUpdateRequest(
        "new name",
        "desc",
        5,
        3,
        ItemManagementType.UNIT,
        true,
        "student id",
        null,
        List.of(new BorrowerRequirementRequest("name", true)),
        true
    );

    AdminItemUpdateResponse response = adminItemService.updateItem(organizationId, itemId, request);

    assertThat(response.name()).isEqualTo("new name");
    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.useMessageAlarmService()).isTrue();
  }

  @Test
  @DisplayName("updateUnitAvailability에서 AVAILABLE -> INACTIVE 전환 시 가능 수량이 감소한다")
  void updateUnitAvailability_availableToInactive() {
    Long organizationId = 1L;
    Long itemId = 10L;
    Long itemUnitId = 100L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "unit item", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    ReflectionTestUtils.setField(item, "totalQuantity", 2);
    ReflectionTestUtils.setField(item, "availableQuantity", 2);
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
  @DisplayName("updateUnitAvailability에서 RENTED 유닛은 전환할 수 없다")
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
        .status(status)
        .build();
    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }
}
