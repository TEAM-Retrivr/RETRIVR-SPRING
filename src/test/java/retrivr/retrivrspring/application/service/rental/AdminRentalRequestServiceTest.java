package retrivr.retrivrspring.application.service.rental;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.infrastructure.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalApproveRequest;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalRejectRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDecisionResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse.RentalRequestSummary;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRentalRequestServiceTest {

  @Mock
  RentalRepository rentalRepository;
  @Mock
  OrganizationRepository organizationRepository;

  @InjectMocks AdminRentalRequestService service;

  @Test
  @DisplayName("getRequestedList: 로그인 조직이 없으면 NOT_FOUND_ORGANIZATION")
  void getRequestedList_orgNotFound_throw() {
    // given
    Long loginOrgId = 1L;
    when(organizationRepository.existsById(loginOrgId)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> service.getRequestedList(loginOrgId, null, 10))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  @Test
  @DisplayName("getRequestedList: size+1 조회 후 hasNext=true면 size로 자르고 nextCursor=마지막 id")
  void getRequestedList_hasNext_true() {
    // given
    Long loginOrgId = 1L;
    Long cursor = null;
    Integer size = 2;

    when(organizationRepository.existsById(loginOrgId)).thenReturn(true);

    Rental r1 = mock(Rental.class);
    Rental r2 = mock(Rental.class);
    Rental r3 = mock(Rental.class);
    when(r2.getId()).thenReturn(20L);

    // repo는 size+1개를 반환
    when(rentalRepository.searchRequestedRentalPage(any(), anyInt(), eq(loginOrgId)))
        .thenReturn(List.of(r1, r2, r3));

    // RentalRequestSummary.from(Rental) static 매핑을 테스트에서는 무시(가짜 반환)
    try (MockedStatic<RentalRequestSummary> mocked = mockStatic(RentalRequestSummary.class)) {
      mocked.when(() -> RentalRequestSummary.from(any(Rental.class)))
          .thenReturn(mock(RentalRequestSummary.class));

      // when
      AdminRentalRequestPageResponse res = service.getRequestedList(loginOrgId, cursor, size);

      // then
      assertThat(res.requests()).hasSize(2);
      assertThat(res.nextCursor()).isEqualTo(20L); // 잘린 리스트의 마지막(r2)

      // size+1로 조회했는지까지 확인하고 싶으면(정규화 정책이 단순하다는 가정 하에)
      verify(rentalRepository).searchRequestedRentalPage(eq(cursor), eq(3), eq(loginOrgId));
    }
  }

  @Test
  @DisplayName("getRequestedList: hasNext=false면 nextCursor=null")
  void getRequestedList_hasNext_false() {
    // given
    Long loginOrgId = 1L;
    Long cursor = 999L;
    Integer size = 3;

    when(organizationRepository.existsById(loginOrgId)).thenReturn(true);

    Rental r1 = mock(Rental.class);
    Rental r2 = mock(Rental.class);

    when(rentalRepository.searchRequestedRentalPage(eq(cursor), anyInt(), eq(loginOrgId)))
        .thenReturn(List.of(r1, r2)); // size보다 적게

    try (MockedStatic<RentalRequestSummary> mocked = mockStatic(RentalRequestSummary.class)) {
      mocked.when(() -> RentalRequestSummary.from(any(Rental.class)))
          .thenReturn(mock(RentalRequestSummary.class));

      // when
      AdminRentalRequestPageResponse res = service.getRequestedList(loginOrgId, cursor, size);

      // then
      assertThat(res.requests()).hasSize(2);
      assertThat(res.nextCursor()).isNull();
    }
  }

  @Test
  @DisplayName("approveRentalRequest: rental 없으면 NOT_FOUND_RENTAL")
  void approveRentalRequest_rentalNotFound_throw() {
    // given
    Long rentalId = 1L;
    Long orgId = 10L;
    AdminRentalApproveRequest req = mock(AdminRentalApproveRequest.class);

    when(rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> service.approveRentalRequest(rentalId, req, orgId))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND_RENTAL));
  }

  @Test
  @DisplayName("approveRentalRequest: organization 없으면 NOT_FOUND_ORGANIZATION")
  void approveRentalRequest_orgNotFound_throw() {
    // given
    Long rentalId = 1L;
    Long orgId = 10L;

    Rental rental = mock(Rental.class);
    when(rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)).thenReturn(Optional.of(rental));
    when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

    AdminRentalApproveRequest req = mock(AdminRentalApproveRequest.class);

    // when & then
    assertThatThrownBy(() -> service.approveRentalRequest(rentalId, req, orgId))
        .isInstanceOf(ApplicationException.class)
        .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION));
  }

  @Test
  @DisplayName("approveRentalRequest: rental.approve 호출되고 응답 status=APPROVE")
  void approveRentalRequest_success() {
    // given
    Long rentalId = 1L;
    Long orgId = 10L;

    Rental rental = mock(Rental.class);
    Organization org = mock(Organization.class);

    when(rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)).thenReturn(Optional.of(rental));
    when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

    AdminRentalApproveRequest req = mock(AdminRentalApproveRequest.class);
    when(req.adminNameToApprove()).thenReturn("adminA");

    // when
    AdminRentalDecisionResponse res = service.approveRentalRequest(rentalId, req, orgId);

    // then
    verify(rental).approve("adminA", org);
    assertThat(res.rentalId()).isEqualTo(rentalId);
    assertThat(res.rentalDecisionStatus().name()).isEqualTo("APPROVE");
    assertThat(res.adminNameToDecide()).isEqualTo("adminA");
    // decidedAt은 LocalDateTime.now()라 정확 비교 대신 null 아닌지만
    assertThat(res.decisionDate()).isNotNull();
  }

  @Test
  @DisplayName("rejectRentalRequest: rental.reject 호출되고 응답 status=REJECT")
  void rejectRentalRequest_success() {
    // given
    Long rentalId = 1L;
    Long orgId = 10L;

    Rental rental = mock(Rental.class);
    Organization org = mock(Organization.class);

    when(rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)).thenReturn(Optional.of(rental));
    when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

    AdminRentalRejectRequest req = mock(AdminRentalRejectRequest.class);
    when(req.adminNameToReject()).thenReturn("adminB");

    // when
    AdminRentalDecisionResponse res = service.rejectRentalRequest(rentalId, req, orgId);

    // then
    verify(rental).reject("adminB", org);
    assertThat(res.rentalId()).isEqualTo(rentalId);
    assertThat(res.rentalDecisionStatus().name()).isEqualTo("REJECT");
    assertThat(res.adminNameToDecide()).isEqualTo("adminB");
    assertThat(res.decisionDate()).isNotNull();
  }
}