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
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemPageResponse;
import retrivr.retrivrspring.presentation.admin.item.res.AdminItemUnitMutationResponse;

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
    ItemUnit unit = createItemUnit(101L, item13, "UNIT-001", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.existsById(organizationId)).thenReturn(true);
    when(itemRepository.findPageByOrganizationWithCursor(organizationId, 20L, 3))
        .thenReturn(List.of(item13, item12, item11));
    when(itemUnitRepository.findAllByItemId(13L)).thenReturn(List.of(unit));

    AdminItemPageResponse response = adminItemService.getItems(organizationId, 20L, 2);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.items().get(0).itemUnits()).hasSize(1);
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
    when(itemBorrowerFieldRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    AdminItemCreateRequest request = new AdminItemCreateRequest(
        "노트북",
        "대여용 노트북",
        7,
        true,
        ItemManagementType.UNIT,
        null
    );

    AdminItemCreateResponse response = adminItemService.createItem(organizationId, request);

    assertThat(response.itemId()).isEqualTo(12L);
    assertThat(response.itemManagementType()).isEqualTo(ItemManagementType.UNIT);
    assertThat(response.borrowerRequirements()).hasSize(3);
  }

  @Test
  @DisplayName("createItem에서 preset field label이 의미와 다르면 예외가 발생한다")
  void createItem_invalidPresetLabel_throws() {
    Long organizationId = 1L;
    Organization organization = createOrganization(organizationId);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

    AdminItemCreateRequest request = new AdminItemCreateRequest(
        "충전기",
        "desc",
        3,
        true,
        ItemManagementType.NON_UNIT,
        List.of(
            new AdminItemCreateRequest.BorrowerRequirement("name", "학과", BorrowerFieldType.TEXT,
                true)
        )
    );

    assertThatThrownBy(() -> adminItemService.createItem(organizationId, request))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST_EXCEPTION));
  }

  @Test
  @DisplayName("updateItem에서 관리 타입 변경은 허용하지 않는다")
  void updateItem_changeManagementType_throws() {
    Long organizationId = 1L;
    Long itemId = 101L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "old", ItemManagementType.NON_UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findFetchItemBorrowerFieldsByIdAndOrganization_Id(itemId, organizationId))
        .thenReturn(Optional.of(item));

    AdminItemUpdateRequest request = new AdminItemUpdateRequest(
        "new name",
        "desc",
        5,
        true,
        ItemManagementType.UNIT,
        null
    );

    assertThatThrownBy(() -> adminItemService.updateItem(organizationId, itemId, request))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST_EXCEPTION));
  }

  @Test
  @DisplayName("updateUnitAvailability에서 AVAILABLE -> INACTIVE 전환 시 가능 수량이 감소한다")
  void updateUnitAvailability_availableToInactive() {
    Long organizationId = 1L;
    Long itemId = 10L;
    Long itemUnitId = 100L;
    Organization organization = createOrganization(organizationId);
    Item item = createItem(itemId, "노트북", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    ReflectionTestUtils.setField(item, "totalQuantity", 2);
    ReflectionTestUtils.setField(item, "availableQuantity", 2);
    ItemUnit itemUnit = createItemUnit(itemUnitId, item, "NB-001", ItemUnitStatus.AVAILABLE);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
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
    Item item = createItem(itemId, "노트북", ItemManagementType.UNIT);
    ReflectionTestUtils.setField(item, "organization", organization);
    ItemUnit itemUnit = createItemUnit(itemUnitId, item, "NB-001", ItemUnitStatus.RENTED);

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    when(itemRepository.findByIdAndOrganization_Id(itemId, organizationId)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findByIdAndItemIdAndItemOrganizationId(itemUnitId, itemId, organizationId))
        .thenReturn(Optional.of(itemUnit));

    assertThatThrownBy(() -> adminItemService.updateUnitAvailability(
        organizationId,
        itemId,
        itemUnitId,
        new AdminItemUnitAvailabilityUpdateRequest(false)
    ))
        .isInstanceOf(RuntimeException.class);
  }

  private Organization createOrganization(Long id) {
    Organization organization = Organization.builder().build();
    ReflectionTestUtils.setField(organization, "id", id);
    return organization;
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

  private ItemUnit createItemUnit(Long id, Item item, String code, ItemUnitStatus status) {
    ItemUnit itemUnit = ItemUnit.builder()
        .item(item)
        .code(code)
        .status(status)
        .build();
    ReflectionTestUtils.setField(itemUnit, "id", id);
    return itemUnit;
  }
}
