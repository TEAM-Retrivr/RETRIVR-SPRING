package retrivr.retrivrspring.application.service.home;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import retrivr.retrivrspring.application.service.admin.home.AdminHomeService;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.RentalItem;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.admin.home.res.AdminHomeResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminHomeServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private AdminHomeService adminHomeService;

    @Test
    @DisplayName("TC-01: 정상 홈 조회 - requestCount + 최신 2건")
    void getHome_success() {

        // given
        Long orgId = 1L;

        Organization org = mock(Organization.class);
        when(org.getName()).thenReturn("건국대학교 도서관자치위원회");
        when(org.getProfileImageKey()).thenReturn(null);

        when(organizationRepository.findById(orgId))
                .thenReturn(java.util.Optional.of(org));

        when(rentalRepository.countByOrganization_IdAndStatus(
                eq(orgId),
                eq(RentalStatus.REQUESTED)
        )).thenReturn(3);

        // mock item
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("8핀 충전기");
        when(item.getAvailableQuantity()).thenReturn(2);
        when(item.getTotalQuantity()).thenReturn(5);

        // mock borrower
        Borrower borrower = mock(Borrower.class);
        when(borrower.getName()).thenReturn("조윤아");

        // mock rentalItem
        RentalItem rentalItem = mock(RentalItem.class);
        when(rentalItem.getItem()).thenReturn(item);

        // mock rental
        Rental rental = mock(Rental.class);
        when(rental.getId()).thenReturn(1001L);
        when(rental.getBorrower()).thenReturn(borrower);
        when(rental.getRequestedAt())
                .thenReturn(LocalDateTime.of(2026, 1, 21, 17, 0));
        when(rental.getRentalItems())
                .thenReturn(List.of(rentalItem));

        when(rentalRepository.findRecentHomeRentalIds(
                eq(orgId),
                eq(RentalStatus.REQUESTED),
                any()
        )).thenReturn(List.of(1001L));
        when(rentalRepository.findRecentHomeRentalsByIds(eq(List.of(1001L))))
                .thenReturn(List.of(rental));

        // when
        AdminHomeResponse result = adminHomeService.getHome(orgId);

        // then
        assertThat(result.organizationName())
                .isEqualTo("건국대학교 도서관자치위원회");
        assertThat(result.profileImageUrl()).isNull();

        assertThat(result.requestCount())
                .isEqualTo(3);

        assertThat(result.recentRequests()).hasSize(1);

        var summary = result.recentRequests().get(0);

        assertThat(summary.rentalId()).isEqualTo(1001L);
        assertThat(summary.itemName()).isEqualTo("8핀 충전기");
        assertThat(summary.availableQuantity()).isEqualTo(2);
        assertThat(summary.totalQuantity()).isEqualTo(5);
        assertThat(summary.borrowerName()).isEqualTo("조윤아");
        assertThat(summary.requestedAt()).isEqualTo(LocalDateTime.of(2026, 1, 21, 17, 0));

        verify(rentalRepository, times(1))
                .countByOrganization_IdAndStatus(orgId, RentalStatus.REQUESTED);

        verify(rentalRepository, times(1))
                .findRecentHomeRentalIds(eq(orgId), eq(RentalStatus.REQUESTED), any());
        verify(rentalRepository, times(1))
                .findRecentHomeRentalsByIds(eq(List.of(1001L)));
    }
}
