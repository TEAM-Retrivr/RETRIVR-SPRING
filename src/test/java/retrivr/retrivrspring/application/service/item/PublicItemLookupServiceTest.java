package retrivr.retrivrspring.application.service.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import retrivr.retrivrspring.application.port.image.ImageStoragePort;
import retrivr.retrivrspring.application.service.open.PublicItemLookupService;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemBorrowerField;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemManagementType;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemDetailResponse;
import retrivr.retrivrspring.presentation.open.item.res.PublicItemListPageResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicItemLookupServiceTest {

  @Mock private ItemRepository itemRepository;
  @Mock private ItemUnitRepository itemUnitRepository;
  @Mock private OrganizationRepository organizationRepository;
  @Mock private ImageStoragePort imageStoragePort;

  @InjectMocks
  private PublicItemLookupService publicItemLookupService;

  private Item mockItem(long id, String name, Integer available, Integer total, boolean active, Integer duration, String description, String guaranteedGoods) {
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
    when(unit.getLabel()).thenReturn("UMB-" + id);
    when(unit.getStatus()).thenReturn(ItemUnitStatus.AVAILABLE);
    return unit;
  }

  private ItemBorrowerField mockBorrowerField(String label, boolean required) {
    ItemBorrowerField borrowerField = mock(ItemBorrowerField.class);
    when(borrowerField.getLabel()).thenReturn(label);
    when(borrowerField.isRequired()).thenReturn(required);
    return borrowerField;
  }

  private Organization mockOrganization(long id, String name) {
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn(id);
    when(org.getName()).thenReturn(name);
    return org;
  }

  @Test
  @DisplayName("IL-01: org not found")
  void listLookup_orgNotFound_throw() {
    Organization mockOrg = mockOrganization(1, "조직1");
    when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> publicItemLookupService.publicOrganizationItemListLookup(1L, null, 10))
        .isInstanceOf(ApplicationException.class)
        .extracting(e -> ((ApplicationException)e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);

    verify(organizationRepository).findById(1L);
    verify(itemRepository, never()).findPageByOrganizationWithCursor(anyLong(), any(), anyInt());
  }

  @Test
  @DisplayName("IL-02: hasNext true")
  void listLookup_hasNext_true() {
    long orgId = 1L;
    Organization mockOrg = mockOrganization(1, "조직1");
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(mockOrg));
    List<Item> fetched = List.of(
        mockItem(100L, "A", 1, 3, true, 7, null, null),
        mockItem(99L, "B", 1, 3, true, 7, null, null),
        mockItem(98L, "C", 1, 3, true, 7, null, null)
    );
    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(3)))
        .thenReturn(fetched);

    PublicItemListPageResponse res = publicItemLookupService.publicOrganizationItemListLookup(orgId, null, 2);

    assertThat(res.items()).hasSize(2);
    assertThat(res.nextCursor()).isEqualTo(99L);
    assertThat(res.organizationId()).isEqualTo(orgId);
    assertThat(res.profileImageUrl()).isNull();
    verifyNoInteractions(imageStoragePort);
  }

  @Test
  @DisplayName("IL-03: 단체 프로필 이미지가 있으면 presigned URL을 반환한다")
  void listLookup_withProfileImage_returnsPresignedUrl() {
    long orgId = 1L;
    Organization mockOrg = mockOrganization(orgId, "조직1");
    when(mockOrg.getProfileImageKey()).thenReturn("organizations/1/profile.png");
    when(organizationRepository.findById(orgId)).thenReturn(Optional.of(mockOrg));
    when(itemRepository.findPageByOrganizationWithCursor(eq(orgId), isNull(), eq(3)))
        .thenReturn(List.of());
    when(imageStoragePort.createPresignedDownloadUrl("organizations/1/profile.png"))
        .thenReturn("https://s3.retrivr/profile-image");

    PublicItemListPageResponse res =
        publicItemLookupService.publicOrganizationItemListLookup(orgId, null, 2);

    assertThat(res.profileImageUrl()).isEqualTo("https://s3.retrivr/profile-image");
    verify(imageStoragePort).createPresignedDownloadUrl("organizations/1/profile.png");
  }

  @Test
  @DisplayName("IL-06: item not found")
  void detailLookup_itemNotFound_throw() {
    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> publicItemLookupService.publicOrganizationItemLookup(10L))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ITEM);
  }

  @Test
  @DisplayName("IL-07: detail maps unit labels")
  void detailLookup_ok() {
    long itemId = 10L;
    Item item = mock(Item.class);
    List<ItemBorrowerField> borrowerFields = List.of(
        mockBorrowerField("학번", true),
        mockBorrowerField("연락처", false)
    );
    List<ItemUnit> units = List.of(
        mockItemUnit(1L),
        mockItemUnit(2L)
    );
    when(item.isUnitType()).thenReturn(true);
    when(item.getItemManagementType()).thenReturn(ItemManagementType.UNIT);
    when(item.getItemBorrowerFields()).thenReturn(borrowerFields);
    Organization org = mockOrganization(1, "조직1");
    when(item.getOrganization()).thenReturn(org);
    when(itemRepository.findFetchItemBorrowerFieldsById(itemId)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findAllByItemId(itemId)).thenReturn(units);

    PublicItemDetailResponse res = publicItemLookupService.publicOrganizationItemLookup(itemId);

    assertThat(res.itemUnits()).hasSize(2);
    assertThat(res.itemUnits().get(0).label()).isEqualTo("UMB-1");
    assertThat(res.borrowerRequirements()).hasSize(2);
    assertThat(res.borrowerRequirements().get(0).label()).isEqualTo("학번");
    assertThat(res.borrowerRequirements().get(0).required()).isTrue();
  }

  @Test
  @DisplayName("IL-08: 탈퇴한 단체의 물건 목록은 조회되지 않는다")
  void listLookup_withdrawnOrg_throw() {
    Organization withdrawnOrg = mockOrganization(1, "탈퇴조직");
    doThrow(new DomainException(ErrorCode.NOT_FOUND_ORGANIZATION))
        .when(withdrawnOrg).assertOperating();
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(withdrawnOrg));

    assertThatThrownBy(() -> publicItemLookupService.publicOrganizationItemListLookup(1L, null, 10))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);

    verify(itemRepository, never()).findPageByOrganizationWithCursor(anyLong(), any(), anyInt());
  }

  @Test
  @DisplayName("IL-09: 탈퇴한 단체의 물건 상세는 조회되지 않는다")
  void detailLookup_withdrawnOrg_throw() {
    Organization withdrawnOrg = mockOrganization(1, "탈퇴조직");
    doThrow(new DomainException(ErrorCode.NOT_FOUND_ORGANIZATION))
        .when(withdrawnOrg).assertOperating();
    Item item = mock(Item.class);
    when(item.getOrganization()).thenReturn(withdrawnOrg);
    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> publicItemLookupService.publicOrganizationItemLookup(10L))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);

    verify(itemUnitRepository, never()).findAllByItemId(anyLong());
  }
}
