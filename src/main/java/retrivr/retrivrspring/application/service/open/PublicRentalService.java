package retrivr.retrivrspring.application.service.open;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.event.RentalApprovedEvent;
import retrivr.retrivrspring.application.event.RentalRejectedEvent;
import retrivr.retrivrspring.application.event.RentalRequestedEvent;
import retrivr.retrivrspring.application.port.id.PublicIdGenerator;
import retrivr.retrivrspring.application.service.admin.auth.AdminCodeVerificationService;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.item.ItemUnitRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalCreateRequest;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalImmediateApproveRequest;
import retrivr.retrivrspring.presentation.open.rental.req.PublicRentalImmediateRejectRequest;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalCreateResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalDetailResponse;

import java.util.HashMap;
import java.util.Map;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalImmediateApproveResponse;
import retrivr.retrivrspring.presentation.open.rental.res.PublicRentalImmediateRejectResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicRentalService {

  private final ObjectMapper objectMapper;
  private final RentalRepository rentalRepository;
  private final ItemRepository itemRepository;
  private final ItemUnitRepository itemUnitRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final PublicIdGenerator publicIdGenerator;
  private final AdminCodeVerificationService adminCodeVerificationService;
  private final PublicPhoneVerificationService publicPhoneVerificationService;

  private static final int MAX_PUBLIC_ID_RETRY = 5;

  @Transactional
  public PublicRentalCreateResponse requestRental(Long itemId, PublicRentalCreateRequest request) {
    // 1. 대여할 Item 조회
    Item targetItem = itemRepository.findFetchItemBorrowerFieldsById(itemId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM));

    publicPhoneVerificationService.validateAndConsumePhoneVerificationToken(request.tokenId(), request.rawToken(), PhoneVerificationPurpose.BORROW);

    // 2. ItemUnit 조회
    ItemUnit targetItemUnit = null;
    if (request.itemUnitId() != null) {
      targetItemUnit = itemUnitRepository.findById(request.itemUnitId())
          .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ITEM_UNIT));
    }

    // 3. itemBorrower Field 를 통해서 request.rentalFields를 검증
    targetItem.validationItemBorrowerFieldsWith(request.renterFields());

    // 4. Borrower 생성
    Borrower borrower = Borrower.create(
        request.name(),
        new PhoneNumber(request.phone()),
        objectMapper.valueToTree(request.renterFields())
    );

    // 5. Rental 생성 및 저장
    String publicId = publicIdGenerator.generateRentalId(targetItem.getOrganization().getId());
    Rental requestedRental = Rental.request(
        targetItem,
        targetItemUnit,
        borrower,
        publicId,
        request.requestNote()
    );

    trySaveRental(requestedRental, targetItem.getOrganization().getId());

    applicationEventPublisher.publishEvent(new RentalRequestedEvent(requestedRental.getId()));

    return new PublicRentalCreateResponse(requestedRental.getId(), targetItem.getId(),
        request.itemUnitId(), requestedRental.getRequestedAt());
  }

  public PublicRentalDetailResponse checkRentalStatusAndDetail(Long rentalId, String token) {
    Rental rental = rentalRepository.findById(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    adminCodeVerificationService.validateAdminCodeVerificationToken(
        rental.getOrganization(), AdminCodeVerificationPurpose.IMMEDIATE_APPROVAL, token);

    //todo: 장바구니? 기능 이후 name List 를 넘기도록 수정
    String itemName = rental.getItem().getName();
    String itemUnitLabel = null;
    if (rental.hasItemUnit()) {
      itemUnitLabel = rental.getItemUnit().getLabel();
    }

    Map<String, String> borrowerField = new HashMap<>();
    if (rental.getBorrower().hasAdditionalInfo()) {
      borrowerField = objectMapper.convertValue(
          rental.getBorrower().getAdditionalBorrowerInfo(),
          new TypeReference<Map<String, String>>() {}
      );
    }

    return PublicRentalDetailResponse.from(rental, itemName, itemUnitLabel, borrowerField);
  }

  private void trySaveRental(Rental rental, Long organizationId) {
    for (int attempt = 0; attempt < MAX_PUBLIC_ID_RETRY; attempt++) {
      try {
        rentalRepository.saveAndFlush(rental);
        return;
      } catch(DataIntegrityViolationException e) {
        log.warn(
            "Rental publicId 충돌 발생. publicId={}, organizationId={}, attempt={}",
            rental.getPublicId(),
            organizationId,
            attempt + 1
        );
      }
      String publicId = publicIdGenerator.generateRentalId(organizationId);
      rental.updatePublicId(publicId);
    }

    throw new ApplicationException(ErrorCode.RENTAL_PUBLIC_ID_GENERATE_FAILED);
  }

  @Transactional
  public PublicRentalImmediateApproveResponse approveRentalRequest(Long rentalId, PublicRentalImmediateApproveRequest request) {
    // 요청된 Rental 조회
    Rental rental = rentalRepository.findFetchRentalItemAndOrganizationByIdWithLock(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    Organization organization = rental.getOrganization();

    // 관리자 코드 인증 토큰 검증
    adminCodeVerificationService.validateAndConsumeAdminCodeVerificationToken(
        organization, AdminCodeVerificationPurpose.IMMEDIATE_APPROVAL, request.adminCodeVerificationToken());

    // 대여 요청 승인
    rental.approve(request.adminNameToApprove(), organization);
    applicationEventPublisher.publishEvent(new RentalApprovedEvent(rental.getId()));

    return new PublicRentalImmediateApproveResponse(organization.getId());
  }

  @Transactional
  public PublicRentalImmediateRejectResponse rejectRentalRequest(Long rentalId, PublicRentalImmediateRejectRequest request) {

    // 1. 대여 정보 조회
    Rental rental = rentalRepository.findFetchRentalItemAndOrganizationByIdWithLock(rentalId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    // 2. 로그인된 조직 조회
    Organization organization = rental.getOrganization();

    // 관리자 코드 인증 토큰 검증
    adminCodeVerificationService.validateAndConsumeAdminCodeVerificationToken(
        organization, AdminCodeVerificationPurpose.IMMEDIATE_APPROVAL, request.adminCodeVerificationToken());


    // 3. 대여 거부
    rental.reject(request.adminNameToReject(), organization);
    applicationEventPublisher.publishEvent(new RentalRejectedEvent(rental.getId()));

    return new PublicRentalImmediateRejectResponse(organization.getId());
  }
}
