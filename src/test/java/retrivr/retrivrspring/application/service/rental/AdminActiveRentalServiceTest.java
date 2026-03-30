package retrivr.retrivrspring.application.service.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import retrivr.retrivrspring.application.event.RentalReturnedEvent;
import retrivr.retrivrspring.application.service.admin.rental.AdminActiveRentalService;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.rental.req.AdminRentalReturnRequest;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminOverdueRentalItemPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminOverdueRentalItemPageResponse.OverdueRentalItemSummary;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalReturnResponse;

@ExtendWith(MockitoExtension.class)
class AdminActiveRentalServiceTest {

  @Mock
  RentalRepository rentalRepository;
  @Mock
  OrganizationRepository organizationRepository;
  @Mock
  ItemRepository itemRepository;
  @Mock
  ItemUnitRepository itemUnitRepository;
  @Mock
  ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  AdminActiveRentalService service;

  @Test
  @DisplayName("getOverdueItemList: cursor를 연체 목록 offset으로 사용하고 다음 offset을 nextCursor로 반환")
  void getOverdueItemList_usesOffsetCursor() {
    Rental r1 = mock(Rental.class);
    Rental r2 = mock(Rental.class);
    Rental r3 = mock(Rental.class);

    when(rentalRepository.searchOverduePageVerified(eq(10L), eq(4L), eq(3), any(LocalDate.class)))
        .thenReturn(List.of(r1, r2, r3));

    try (MockedStatic<OverdueRentalItemSummary> mocked = mockStatic(OverdueRentalItemSummary.class)) {
      mocked.when(() -> OverdueRentalItemSummary.from(any(Rental.class)))
          .thenReturn(mock(OverdueRentalItemSummary.class));

      AdminOverdueRentalItemPageResponse response = service.getOverdueItemList(10L, 4L, 2);

      assertThat(response.rentals()).hasSize(2);
      assertThat(response.nextCursor()).isEqualTo(6L);
      verify(rentalRepository).searchOverduePageVerified(eq(10L), eq(4L), eq(3), any(LocalDate.class));
    }
  }

  @Test
  @DisplayName("getOverdueItemList: 마지막 페이지면 nextCursor는 null")
  void getOverdueItemList_lastPageHasNoNextCursor() {
    Rental r1 = mock(Rental.class);
    Rental r2 = mock(Rental.class);

    when(rentalRepository.searchOverduePageVerified(eq(10L), isNull(), eq(3), any(LocalDate.class)))
        .thenReturn(List.of(r1, r2));

    try (MockedStatic<OverdueRentalItemSummary> mocked = mockStatic(OverdueRentalItemSummary.class)) {
      mocked.when(() -> OverdueRentalItemSummary.from(any(Rental.class)))
          .thenReturn(mock(OverdueRentalItemSummary.class));

      AdminOverdueRentalItemPageResponse response = service.getOverdueItemList(10L, null, 2);

      assertThat(response.rentals()).hasSize(2);
      assertThat(response.nextCursor()).isNull();
      verify(rentalRepository).searchOverduePageVerified(eq(10L), isNull(), eq(3), any(LocalDate.class));
    }
  }

  @Test
  @DisplayName("confirmReturn: rental not found")
  void confirmReturn_rentalNotFound() {
    when(rentalRepository.findByIdWithItems(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirmReturn(10L, 1L, new AdminRentalReturnRequest("admin")))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND_RENTAL));
  }

  @Test
  @DisplayName("confirmReturn: success publishes return confirmed event")
  void confirmReturn_success() {
    Rental rental = mock(Rental.class);
    Organization organization = mock(Organization.class);

    when(rentalRepository.findByIdWithItems(1L)).thenReturn(Optional.of(rental));
    when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
    when(rental.getId()).thenReturn(1L);

    AdminRentalReturnResponse response =
        service.confirmReturn(10L, 1L, new AdminRentalReturnRequest("admin"));

    verify(rental).markReturned("admin", organization);
    verify(applicationEventPublisher).publishEvent(new RentalReturnedEvent(1L));
    assertThat(response.rentalId()).isEqualTo(1L);
    assertThat(response.rentalStatus()).isEqualTo(RentalStatus.RETURNED);
    assertThat(response.adminNameToConfirm()).isEqualTo("admin");
  }
}
