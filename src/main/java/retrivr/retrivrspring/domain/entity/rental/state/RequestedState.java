package retrivr.retrivrspring.domain.entity.rental.state;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestedState implements RentalState {

  public static final RequestedState INSTANCE = new RequestedState();

  @Override
  public void approve(Rental rental, String adminName, Organization loginOrganization) {
    // 1. 대여가 로그인한 조직의 소유인지 확인
    rental.validateRentalOwner(loginOrganization);

    // 2. Item, ItemUnit 조회 및 승인 이벤트 트리거
    Item item = rental.getItem();
    item.onRentalApprove(rental.getItemUnit());

    LocalDateTime now = LocalDateTime.now();
    LocalDate dueDate = now.plusDays(item.getRentalDuration()).toLocalDate();

    // 3. Rented 상태로 변경
    rental.setRented(adminName, now, dueDate);
  }

  @Override
  public void reject(Rental rental, String adminName, Organization loginOrganization) {
    // 1. 대여가 로그인한 조직의 소유인지 확인
    rental.validateRentalOwner(loginOrganization);

    // 2. Item 조회 및 거부 이벤트 트리거
    Item item = rental.getItem();
    item.onRentalRejected(rental.getItemUnit());

    // 3. Reject 상태로 변경
    rental.setReject(adminName, LocalDateTime.now());
  }


  /**
   *
   * Transition 불가 함수
   *
   */
  @Override
  public void changeDueDate(Rental rental, LocalDate newDueDate, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot return when REQUESTED");
  }

  @Override
  public void markReturned(Rental rental, String adminName, Organization org) {
    throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION,
        "Cannot return when REQUESTED");
  }
}
