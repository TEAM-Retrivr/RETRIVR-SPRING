package retrivr.retrivrspring.application.service.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.ApplicationEventPublisher;
import retrivr.retrivrspring.application.event.RentalRequestedEvent;
import retrivr.retrivrspring.application.port.id.PublicIdGenerator;
import retrivr.retrivrspring.application.service.open.PublicRentalService;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentalItem;
import retrivr.retrivrspring.domain.entity.rental.RentalItemUnit;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalDetailResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicRentalServiceTest {

  @Mock private RentalRepository rentalRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private ItemUnitRepository itemUnitRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private PublicIdGenerator publicIdGenerator;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private PublicRentalService service() {
    return new PublicRentalService(
        objectMapper,
        rentalRepository,
        itemRepository,
        itemUnitRepository,
        applicationEventPublisher,
        publicIdGenerator
    );
  }

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

  private ItemUnit mockUnit(Long itemId, Long unitId, boolean rentalAble, String label) {
    Item item = mock(Item.class);
    when(item.getId()).thenReturn(itemId);
    ItemUnit unit = mock(ItemUnit.class);
    when(unit.getId()).thenReturn(unitId);
    when(unit.isRentalAble()).thenReturn(rentalAble);
    when(unit.getLabel()).thenReturn(label);
    when(unit.getItem()).thenReturn(item);
    return unit;
  }

  @Test
  @DisplayName("PR-01: item not found")
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
  @DisplayName("PR-05: borrower field validation fail")
  void requestRental_borrowerFieldValidationFail() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);
    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(null);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    doThrow(new DomainException(ErrorCode.ILLEGAL_BORROWER_LABEL, "bad fields"))
        .when(item).validationItemBorrowerFieldsWith(anyMap());

    assertThatThrownBy(() -> service().requestRental(10L, req))
        .isInstanceOf(DomainException.class);
  }

  @Test
  @DisplayName("PR-06: success without unit")
  void requestRental_success_withoutUnit() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);
    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(null);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    doNothing().when(item).validationItemBorrowerFieldsWith(anyMap());
    ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
    doAnswer(inv -> {
      Rental savedRental = inv.getArgument(0);
      setRentalId(savedRental, 1L);
      return null;
    }).when(rentalRepository).save(any(Rental.class));

    PublicRentalCreateResponse res = service().requestRental(10L, req);

    verify(rentalRepository, times(1)).save(rentalCaptor.capture());
    verify(applicationEventPublisher).publishEvent(new RentalRequestedEvent(1L));
    assertThat(res.itemId()).isEqualTo(10L);
    assertThat(res.itemUnitId()).isNull();
    assertThat(res.requestedAt()).isNotNull();
  }

  @Test
  @DisplayName("PR-07: success with unit")
  void requestRental_success_withUnit() {
    Organization org = mockOrg(1L);
    Item item = mockItem(10L, true, org);
    ItemUnit unit = mockUnit(10L, 99L, true, "unit-99");

    when(itemRepository.findFetchItemBorrowerFieldsById(10L)).thenReturn(Optional.of(item));
    when(itemUnitRepository.findById(99L)).thenReturn(Optional.of(unit));

    PublicRentalCreateRequest req = mock(PublicRentalCreateRequest.class);
    when(req.itemUnitId()).thenReturn(99L);
    when(req.renterFields()).thenReturn(Map.of("학과", "컴공"));
    when(req.name()).thenReturn("홍길동");
    when(req.phone()).thenReturn("010-0000-0000");

    doNothing().when(item).validationItemBorrowerFieldsWith(anyMap());
    doAnswer(inv -> {
      Rental savedRental = inv.getArgument(0);
      setRentalId(savedRental, 1L);
      return null;
    }).when(rentalRepository).save(any(Rental.class));

    PublicRentalCreateResponse res = service().requestRental(10L, req);

    verify(rentalRepository, times(1)).save(any(Rental.class));
    verify(applicationEventPublisher).publishEvent(new RentalRequestedEvent(1L));
    assertThat(res.itemId()).isEqualTo(10L);
    assertThat(res.itemUnitId()).isEqualTo(99L);
  }

  @Test
  @DisplayName("PR-08: rental not found")
  void checkRental_notFound() {
    when(rentalRepository.findById(777L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().checkRentalStatusAndDetail(777L))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  @DisplayName("PR-09: detail without unit")
  void checkRental_detail_withoutUnit() {
    Rental rental = mock(Rental.class);
    when(rental.getId()).thenReturn(1L);
    when(rental.getStatus()).thenReturn(RentalStatus.REQUESTED);
    when(rental.getDecidedAt()).thenReturn((LocalDateTime) null);
    when(rental.getDueDate()).thenReturn((LocalDate) null);

    Item item = mock(Item.class);
    when(item.getName()).thenReturn("카메라");
    RentalItem rentalItem = mock(RentalItem.class);
    when(rentalItem.getItem()).thenReturn(item);
    when(rental.getRentalItems()).thenReturn(List.of(rentalItem));
    when(rental.getRentalItemUnits()).thenReturn(List.of());

    Borrower borrower = mock(Borrower.class);
    JsonNode info = objectMapper.valueToTree(Map.of("학과", "컴공"));
    when(borrower.getAdditionalBorrowerInfo()).thenReturn(info);
    when(borrower.hasAdditionalInfo()).thenReturn(true);
    when(rental.getBorrower()).thenReturn(borrower);

    when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

    PublicRentalDetailResponse res = service().checkRentalStatusAndDetail(1L);

    assertThat(res.itemName()).isEqualTo("카메라");
    assertThat(res.itemUnitLabel()).isNull();
    assertThat(res.borrowerField()).containsEntry("학과", "컴공");
  }

  @Test
  @DisplayName("PR-10: detail with unit")
  void checkRental_detail_withUnit() {
    Rental rental = mock(Rental.class);
    when(rental.getId()).thenReturn(2L);
    when(rental.getStatus()).thenReturn(RentalStatus.RENTED);
    when(rental.getDecidedAt()).thenReturn(LocalDateTime.now());
    when(rental.getDueDate()).thenReturn(LocalDate.now().plusDays(7));

    Item item = mock(Item.class);
    when(item.getName()).thenReturn("노트북");
    RentalItem rentalItem = mock(RentalItem.class);
    when(rentalItem.getItem()).thenReturn(item);
    when(rental.getRentalItems()).thenReturn(List.of(rentalItem));

    ItemUnit unit = mock(ItemUnit.class);
    when(unit.getLabel()).thenReturn("unit-001");
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
    assertThat(res.itemUnitLabel()).isEqualTo("unit-001");
    assertThat(res.borrowerField()).containsEntry("학번", "20251234");
  }
  private void setRentalId(Rental rental, Long rentalId) {
    try {
      java.lang.reflect.Field idField = Rental.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(rental, rentalId);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
