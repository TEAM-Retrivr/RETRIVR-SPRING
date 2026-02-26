package retrivr.retrivrspring.application.service.rental;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalDecisionStatus;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.repository.rental.RentalRepository;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalApproveRequest;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalRejectRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDecisionResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse.RentalRequestSummary;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalRequestService {

  private final RentalRepository rentalRepository;
  private final OrganizationRepository organizationRepository;

  public AdminRentalRequestPageResponse getRequestedList(Long loginOrganizationId, Long cursor, Integer size) {
    if (!organizationRepository.existsById(loginOrganizationId)) {
      throw new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION);
    }

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(size);

    List<Rental> requestedRentalPage = rentalRepository.searchRequestedRentalPage(cursor,
        normalizedSize.sizePlusOne(), loginOrganizationId);

    boolean hasNext = requestedRentalPage.size() > normalizedSize.size();
    requestedRentalPage = hasNext ? requestedRentalPage.subList(0, normalizedSize.size()) : requestedRentalPage;

    Long nextCursor = null;
    if (hasNext) {
      nextCursor = requestedRentalPage.getLast().getId();
    }

    List<RentalRequestSummary> content = requestedRentalPage.stream()
        .map(RentalRequestSummary::from)
        .toList();

    return new AdminRentalRequestPageResponse(content, nextCursor);
  }

  @Transactional
  public AdminRentalDecisionResponse approveRentalRequest(Long rentalId,
      AdminRentalApproveRequest request, Long mockOrganizationId) {
    Rental rental = rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    Organization organizationToApprove = organizationRepository.findById(mockOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    rental.approve(request.adminNameToApprove(), organizationToApprove);

    return new AdminRentalDecisionResponse(
        rentalId,
        RentalDecisionStatus.APPROVE,
        request.adminNameToApprove(),
        LocalDateTime.now()
    );
  }

  @Transactional
  public AdminRentalDecisionResponse rejectRentalRequest(Long rentalId,
      AdminRentalRejectRequest request, Long mockOrganizationId) {
    Rental rental = rentalRepository.findFetchRentalItemAndOrganizationById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    Organization organizationToApprove = organizationRepository.findById(mockOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    rental.reject(request.adminNameToReject(), organizationToApprove);

    return new AdminRentalDecisionResponse(
        rentalId,
        RentalDecisionStatus.REJECT,
        request.adminNameToReject(),
        LocalDateTime.now()
    );
  }
}
