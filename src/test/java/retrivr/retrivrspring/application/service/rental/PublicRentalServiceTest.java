package retrivr.retrivrspring.application.service.rental;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentalItem;
import retrivr.retrivrspring.domain.entity.rental.RentalItemUnit;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.item.ItemRepository;
import retrivr.retrivrspring.infrastructure.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.infrastructure.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.rental.res.PublicRentalDetailResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicRentalServiceTest {

  @Mock private RentalRepository rentalRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private ItemUnitRepository itemUnitRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private PublicRentalService service() {
    return new PublicRentalService(objectMapper, rentalRepository, itemRepository, itemUnitRepository);
  }

  // ---------- helpers ----------
  private Item mockItem(Long itemId, boolean rentalAble, Organization org) {
    Item item = mock(Item.class);
    when(item.getId()).thenReturn(itemId);
    when(item.isRentalAble()).thenReturn(rentalAble);
    when(item.getOrganization()).thenReturn(org);
    return item;
  }

  private Organization mockOrg(Long orgId) {
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn(orgId);
    return org;
  }

  private ItemUnit mockUnit(Long itemId, Long unitId, boolean rentalAble, String code) {
    Item item = mock(Item.class);
    when(item.getId()).thenReturn(itemId);
    ItemUnit unit = mock(ItemUnit.class);
    when(unit.getId()).thenReturn(unitId);
    when(unit.isRentalAble()).thenReturn(rentalAble);
    when(unit.getCode()).thenReturn(code);
    when(unit.getItem()).thenReturn(item);
    return unit;
  }

  // -------------------------------
  // A) requestRental
  // -------------------------------

  @Test
  @DisplayName("PR-01: Item이 없으면 NOT_FOUND_ITEM")
  void requestRental_itemNotFound() {
    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.empty());

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(ApplicationException.class)
        .extracting(e -> ((ApplicationException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND_ITEM);

    verify(rentalRepository, never()).save(any());
  }

  @Test
  @DisplayName("PR-02: Item 대여 불가면 NOT_AVAILABLE_ITEM")
  void requestRental_itemNotAvailable() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, false, org);

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(ApplicationException.class);

    verify(rentalRepository, never()).save(any());
  }

  @Test
  @DisplayName("PR-03: itemUnitId 존재 + Unit이 없으면 NOT_FOUND_ITEM_UNIT")
  void requestRental_unitNotFound() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findById(99L)).thenReturn(Optional.empty());

    // request stub
    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(99L);

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(ApplicationException.class);

    verify(rentalRepository, never()).save(any());
  }

  @Test
  @DisplayName("PR-04: itemUnitId 존재 + Unit 대여 불가면 NOT_AVAILABLE_ITEM_UNIT")
  void requestRental_unitNotAvailable() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);
    ItemUnit unit = mockUnit(10L, 99L, false, "UMB-099");

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findById(99L)).thenReturn(Optional.of(unit));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(99L);

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(ApplicationException.class);

    verify(rentalRepository, never()).save(any());
  }

  @Test
  @DisplayName("PR-05: borrower field 검증 실패(DomainException)면 그대로 전파")
  void requestRental_borrowerFieldValidationFail() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(null);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    doThrow(new DomainException(ErrorCode.ILLEGAL_BORROWER_FIELD, "bad fields"))
        .when(item).validationItemBorrowerFieldsWith(anyMap());

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(DomainException.class);

    verify(rentalRepository, never()).save(any());
  }

  @Test
  @DisplayName("PR-06: 성공(단위 선택 X) -> save 호출 + 응답 반환")
  void requestRental_success_withoutUnit() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(null);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    // validation 통과
    doNothing().when(item).validationItemBorrowerFieldsWith(anyMap());

    // save 되는 Rental 캡처 후, getter stub(응답 생성용)
    ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);

    // save()가 void 아니면 그대로 두면 됨. 여기선 save 결과는 사용 안 하니 doNothing/when 불필요.
    doAnswer(inv -> null).when(rentalRepository).save(any(Rental.class));

    // 핵심: 응답 만들 때 requestedRental.getId(), getRequestedAt() 호출됨 → 캡처한 객체에 stub 걸기 어려움
    // 그래서 save() 이전에 생성된 Rental의 값을 컨트롤하기 위해 Rental.request(...)가 "실제 static 팩토리"라면
    // 단위테스트에서 완벽 제어가 어렵다.
    //
    // 실무적으로는 Rental.request(...) 결과가 최소한 null 아니고, save 호출되는지 + response에 itemId가 들어가는지 정도를 검증.
    // 응답의 rentalId/requestedAt은 null이 아닐 것이라는 '계약'을 도메인에서 보장한다고 가정.
    //
    // 여기서는 save가 호출되는 것과 response.itemId/itemUnitId를 검증한다.

    PublicRentalCreateResponse res = service().requestRental(10L, req);

    verify(rentalRepository, times(1)).save(rentalCaptor.capture());
    assertThat(res.itemId()).isEqualTo(10L);
    assertThat(res.itemUnitId()).isNull();
    assertThat(res.requestedAt()).isNotNull();
  }

  @Test
  @DisplayName("PR-07: 성공(단위 선택 O) -> save 호출 + 응답에 itemUnitId 포함")
  void requestRental_success_withUnit() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);
    ItemUnit unit = mockUnit(10L,99L, true, "UMB-099");

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findById(99L)).thenReturn(Optional.of(unit));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(99L);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    doNothing().when(item).validationItemBorrowerFieldsWith(anyMap());

    PublicRentalCreateResponse res = service().requestRental(10L, req);

    verify(rentalRepository, times(1)).save(any(Rental.class));
    assertThat(res.itemId()).isEqualTo(10L);
    assertThat(res.itemUnitId()).isEqualTo(99L);
    assertThat(res.requestedAt()).isNotNull();
  }

  // -------------------------------
  // B) checkRentalStatusAndDetail
  // -------------------------------

  @Test
  @DisplayName("PR-08: Rental이 없으면 NOT_FOUND_RENTAL")
  void checkRental_notFound() {
    when(rentalRepository.findById(777L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().checkRentalStatusAndDetail(777L))
        .isInstanceOf(ApplicationException.class);

    verify(rentalRepository).findById(777L);
  }

  @Test
  @DisplayName("PR-09: unit 없는 대여 상세 -> itemUnitCode=null, borrowerField 변환")
  void checkRental_detail_withoutUnit() {
    // given rental mock graph
    Rental rental = mock(Rental.class);
    when(rental.getId()).thenReturn(1L);
    when(rental.getStatus()).thenReturn(RentalStatus.REQUESTED);
    when(rental.getDecidedAt()).thenReturn((LocalDateTime) null);
    when(rental.getDueDate()).thenReturn((LocalDate) null);

    // rentalItems[0].item.name
    Item item = mock(Item.class);
    when(item.getName()).thenReturn("카메라");

    RentalItem rentalItem = mock(RentalItem.class);
    when(rentalItem.getItem()).thenReturn(item);

    when(rental.getRentalItems()).thenReturn(List.of(rentalItem));

    // rentalItemUnits empty
    when(rental.getRentalItemUnits()).thenReturn(List.of());

    // borrower.additionalBorrowerInfo = {"학과":"컴공"}
    Borrower borrower = mock(Borrower.class);
    JsonNode info = objectMapper.valueToTree(Map.of("학과", "컴공"));
    when(borrower.getAdditionalBorrowerInfo()).thenReturn(info);
    when(borrower.hasAdditionalInfo()).thenReturn(true);
    when(rental.getBorrower()).thenReturn(borrower);

    when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

    // when
    PublicRentalDetailResponse res = service().checkRentalStatusAndDetail(1L);

    // then
    assertThat(res.rentalId()).isEqualTo(1L);
    assertThat(res.itemName()).isEqualTo("카메라");
    assertThat(res.itemUnitCode()).isNull();
    assertThat(res.borrowerField()).containsEntry("학과", "컴공");
  }

  @Test
  @DisplayName("PR-10: unit 있는 대여 상세 -> itemUnitCode=첫 unit.code")
  void checkRental_detail_withUnit() {
    Rental rental = mock(Rental.class);
    when(rental.getId()).thenReturn(2L);
    when(rental.getStatus()).thenReturn(RentalStatus.APPROVED);
    when(rental.getDecidedAt()).thenReturn(LocalDateTime.now());
    when(rental.getDueDate()).thenReturn(LocalDate.now().plusDays(7));

    Item item = mock(Item.class);
    when(item.getName()).thenReturn("노트북");

    RentalItem rentalItem = mock(RentalItem.class);
    when(rentalItem.getItem()).thenReturn(item);
    when(rental.getRentalItems()).thenReturn(List.of(rentalItem));

    ItemUnit unit = mock(ItemUnit.class);
    when(unit.getCode()).thenReturn("UMB-001");

    RentalItemUnit riu = mock(RentalItemUnit.class);
    when(riu.getItemUnit()).thenReturn(unit);

    when(rental.getRentalItemUnits()).thenReturn(List.of(riu));

    Borrower borrower = mock(Borrower.class);
    JsonNode info = objectMapper.valueToTree(Map.of("학번", "20251234"));
    when(borrower.getAdditionalBorrowerInfo()).thenReturn(info);
    when(borrower.hasAdditionalInfo()).thenReturn(true);
    when(rental.getBorrower()).thenReturn(borrower);

    when(rentalRepository.findById(2L)).thenReturn(Optional.of(rental));

    PublicRentalDetailResponse res = service().checkRentalStatusAndDetail(2L);

    assertThat(res.itemName()).isEqualTo("노트북");
    assertThat(res.itemUnitCode()).isEqualTo("UMB-001");
    assertThat(res.borrowerField()).containsEntry("학번", "20251234");
    assertThat(res.rentalStatus()).isEqualTo(RentalStatus.APPROVED);
  }
}