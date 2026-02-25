package retrivr.retrivrspring.application.service.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.item.ItemRepository;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.presentation.item.res.PublicItemDetailResponse;
import retrivr.retrivrspring.presentation.item.res.PublicItemListPageResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemLookupServiceTest {

  @Mock
  private ItemRepository itemRepository;
  @Mock
  private ItemUnitRepository itemUnitRepository;
  @Mock
  private OrganizationRepository organizationRepository;

  @InjectMocks
  private ItemLookupService itemLookupService;

  private Item mockItem(
      long id,
      String name,
      Integer available,
      Integer total,
      boolean active,
      Integer duration,
      String description,
      String guaranteedGoods
  ) {
    Item item = mock(Item.class);
    when(item.getId()).thenReturn(id);
    when(item.getName()).thenReturn(name);
    when(item.getAvailableQuantity()).thenReturn(available);
    when(item.getTotalQuantity()).thenReturn(total);
    when(item.isActive()).thenReturn(active);
    when(item.getRentalDuration()).thenReturn(duration);
    when(item.getDescription()).thenReturn(description);
    when(item.getGuaranteedGoods()).thenReturn(guaranteedGoods);
    return item;
  }

  private ItemUnit mockItemUnit(long id) {
    ItemUnit unit = mock(ItemUnit.class);
    when(unit.getId()).thenReturn(id);
    when(unit.getCode()).thenReturn("UMB-" + id);
    when(unit.getStatus()).thenReturn(ItemUnitStatus.AVAILABLE);

    return unit;
  }

  // -------------------------
  // A) publicOrganizationItemListLookup
  // -------------------------

  @Test
  @DisplayName("IL-01: 조직이 존재하지 않으면 NOT_FOUND_ORGANIZATION 예외")
  void listLookup_orgNotFound_throw() {
    // given
    when(organizationRepository.existsById(1L)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> itemLookupService.publicOrganizationItemListLookup(1L, null, 10))
        .isInstanceOf(ApplicationException.class)
        .extracting(e -> ((ApplicationException)e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);

    verify(organizationRepository).existsById(1L);
    verify(itemRepository, never()).findPageByOrganizationWithCursor(anyLong(), any(), anyInt());
  }

  @Test
  @DisplayName("IL-02: hasNext=true이면 size 만큼 반환하고 nextCursor는 마지막 itemId")
  void listLookup_hasNext_true() {
    // given
    long orgId = 1L;
    Long cursor = null;
    int size = 2;

    when(organizationRepository.existsById(orgId)).thenReturn(true);

    List<Item> fetched = List.of(
        mockItem(100L, "A", 1, 3, true, 7, null, null),
        mockItem(99L, "B", 1, 3, true, 7, null, null),
        mockItem(98L, "C", 1, 3, true, 7, null, null) // size+1
    );

    // thenReturn 안에서 mock 생성하지 않음 (이미 만들어 둠)
    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(size + 1)))
        .thenReturn(fetched);

    // when
    PublicItemListPageResponse res = itemLookupService.publicOrganizationItemListLookup(orgId,
        cursor, size);

    // then
    assertThat(res.items()).hasSize(size);
    assertThat(res.nextCursor()).isEqualTo(99L); // page의 last id (100,99)

    verify(itemRepository).findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(size + 1));
  }

  @Test
  @DisplayName("IL-03: hasNext=false이면 전체 반환하고 nextCursor=null")
  void listLookup_hasNext_false() {
    // given
    long orgId = 1L;
    Long cursor = null;
    int size = 3;

    when(organizationRepository.existsById(orgId)).thenReturn(true);

    List<Item> fetched = List.of(
        mockItem(100L, "A", 1, 3, true, 7, null, null),
        mockItem(99L, "B", 1, 3, true, 7, null, null),
        mockItem(98L, "C", 1, 3, true, 7, null, null)

        // size+1보다 작음 → hasNext=false
    );

    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(size + 1)))
        .thenReturn(fetched);

    // when
    PublicItemListPageResponse res = itemLookupService.publicOrganizationItemListLookup(orgId,
        cursor, size);

    // then
    assertThat(res.items()).hasSize(size);
    assertThat(res.nextCursor()).isNull();
  }

  @Test
  @DisplayName("IL-04: size<=0이면 DEFAULT(15) 적용되어 repo limit=16으로 호출")
  void listLookup_sizeDefault() {
    // given
    long orgId = 1L;
    when(organizationRepository.existsById(orgId)).thenReturn(true);

    // DEFAULT 15라서 sizePlusOne=16
    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(16)))
        .thenReturn(List.of()); // 비어있어도 OK

    // when
    PublicItemListPageResponse res = itemLookupService.publicOrganizationItemListLookup(orgId, null,
        0);

    // then
    assertThat(res.items()).isEmpty();
    assertThat(res.nextCursor()).isNull();
    verify(itemRepository).findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(16));
  }

  @Test
  @DisplayName("IL-05: size>MAX(50)이면 clamp되어 repo limit=51로 호출")
  void listLookup_sizeClampMax() {
    // given
    long orgId = 1L;
    when(organizationRepository.existsById(orgId)).thenReturn(true);

    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(51)))
        .thenReturn(List.of());

    // when
    PublicItemListPageResponse res = itemLookupService.publicOrganizationItemListLookup(orgId, null,
        100);

    // then
    assertThat(res.items()).isEmpty();
    assertThat(res.nextCursor()).isNull();
    verify(itemRepository).findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(51));
  }

  // -------------------------
  // B) publicOrganizationItemLookup
  // -------------------------

  @Test
  @DisplayName("IL-06: item 존재하지 않으면 NOT_FOUND_ITEM 예외")
  void detailLookup_itemNotFound_throw() {
    // given
    when(itemRepository.existsById(10L)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> itemLookupService.publicOrganizationItemLookup(10L))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode") // ApplicationException 구조에 맞게 수정
        .isEqualTo(ErrorCode.NOT_FOUND_ITEM);

    verify(itemUnitRepository, never()).findAllByItemId(anyLong());
  }

  @Test
  @DisplayName("IL-07: item 존재하면 itemUnit 목록을 PublicItemDetailResponse로 매핑")
  void detailLookup_ok() {
    // given
    long itemId = 10L;
    Item item = mock(Item.class);
    when(item.isUnitType()).thenReturn(true);
    when(item.getItemManagementType()).thenReturn(ItemManagementType.UNIT);
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

    List<ItemUnit> units = List.of(
        mockItemUnit(1L),
        mockItemUnit(2L)
    );

    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(units);

    // when
    PublicItemDetailResponse res = itemLookupService.publicOrganizationItemLookup(itemId);

    // then
    // PublicItemDetailResponse의 필드명이 뭔지에 따라 아래 접근은 수정 필요
    // 예: res.units() / res.items() 등
    assertThat(res.itemUnits()).hasSize(2);

    verify(itemUnitRepository).findAllByItemId(itemId);
  }
}