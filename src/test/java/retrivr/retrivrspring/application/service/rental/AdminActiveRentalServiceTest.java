package retrivr.retrivrspring.application.service.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.application.service.admin.rental.AdminActiveRentalService;
import retrivr.retrivrspring.application.service.message.SendMessageService;
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
  SendMessageService sendMessageService;

  @InjectMocks
  AdminActiveRentalService service;

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
  @DisplayName("confirmReturn: success sends return confirmed email")
  void confirmReturn_success() {
    Rental rental = mock(Rental.class);
    Organization organization = mock(Organization.class);

    when(rentalRepository.findByIdWithItems(1L)).thenReturn(Optional.of(rental));
    when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

    AdminRentalReturnResponse response =
        service.confirmReturn(10L, 1L, new AdminRentalReturnRequest("admin"));

    verify(rental).markReturned("admin", organization);
    verify(sendMessageService).sendReturnConfirmed(rental);
    assertThat(response.rentalId()).isEqualTo(1L);
    assertThat(response.rentalStatus()).isEqualTo(RentalStatus.RETURNED);
    assertThat(response.adminNameToConfirm()).isEqualTo("admin");
  }
}
