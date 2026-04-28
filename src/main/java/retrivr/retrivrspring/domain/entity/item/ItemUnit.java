package retrivr.retrivrspring.domain.entity.item;

import jakarta.persistence.*;
import lombok.*;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.enumerate.ItemUnitStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item_unit", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"item_id", "label"})
})
public class ItemUnit extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_unit_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Column(nullable = false, length = 255)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemUnitStatus status;

  /**
   * 현재 유닛이 대여 가능한 상태인지 확인한다.
   * AVAILABLE 상태일 때만 새 대여 요청을 받을 수 있다.
   */
  public boolean isRentalAble() {
    validateStatusExists();
    return this.status == ItemUnitStatus.AVAILABLE;
  }

  /**
   * 대여 경합 중이거나 대여중인 상태를 제외하고 이용 가능하지 않은 상태인지 판단
   */
  public boolean isUnavailable() {
    validateStatusExists();
    return this.status == ItemUnitStatus.LOST || this.status == ItemUnitStatus.BROKEN || this.status == ItemUnitStatus.INACTIVE;
  }

  /**
   * 대여 요청 처리.
   * AVAILABLE 상태의 유닛만 RENTAL_PENDING 상태로 전이할 수 있다.
   */
  public void onRentalRequested() {
    transitionToRentalPendingStatus();
  }

  /**
   * 대여 거절 처리.
   * 요청 중이던 유닛을 다시 AVAILABLE 상태로 복구한다.
   */
  public void onRentalRejected() {
    transitionToAvailableStatus();
  }

  /**
   * 대여 승인 처리.
   * 요청 중(RENTAL_PENDING)인 유닛만 실제 대여 중(RENTED) 상태로 전이할 수 있다.
   */
  public void onRentalApprove() {
    transitionToRentedStatus();
  }

  /**
   * 대여 승인 처리.
   * 요청 중(RENTAL_PENDING)인 유닛만 실제 대여 중(RENTED) 상태로 전이할 수 있다.
   */
  public void onRentalReturned() {
    transitionToAvailableStatus();
  }



  public void changeAvailability(boolean isAvailable) {
    validateStatusExists();

    if (this.status == ItemUnitStatus.RENTED || this.status == ItemUnitStatus.RENTAL_PENDING) {
      throw new DomainException(
          ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot change availability for rented item unit"
      );
    }

    if (isAvailable) {
      if (this.status != ItemUnitStatus.INACTIVE) {
        throw new DomainException(
            ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
            "Cannot transition to AVAILABLE from status: " + this.status
        );
      }
      this.status = ItemUnitStatus.AVAILABLE;
      return;
    }

    if (this.status != ItemUnitStatus.AVAILABLE) {
      throw new DomainException(
          ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to INACTIVE from status: " + this.status
      );
    }
    this.status = ItemUnitStatus.INACTIVE;
  }
  /**
   * 현재 유닛을 RENTED 상태로 전이한다.
   * RENTAL_PENDING 상태에서만 허용된다.
   */
  private void transitionToRentedStatus() {
    validateStatusExists();

    if (this.status != ItemUnitStatus.RENTAL_PENDING) {
      throw new DomainException(
          ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to RENTED from status: " + this.status
      );
    }
    this.status = ItemUnitStatus.RENTED;
  }

  /**
   * 현재 유닛을 RENTAL_PENDING 상태로 전이한다.
   * AVAILABLE 상태에서만 허용된다.
   */
  private void transitionToRentalPendingStatus() {
    validateStatusExists();

    if (this.status != ItemUnitStatus.AVAILABLE) {
      throw new DomainException(
          ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to RENTAL_PENDING from status: " + this.status
      );
    }
    this.status = ItemUnitStatus.RENTAL_PENDING;
  }

  /**
   * 현재 유닛을 AVAILABLE 상태로 전이한다.
   * RENTAL_PENDING 또는 RENTED 상태에서만 허용된다.
   */
  private void transitionToAvailableStatus() {
    validateStatusExists();

    if (this.status != ItemUnitStatus.RENTAL_PENDING && this.status != ItemUnitStatus.RENTED) {
      throw new DomainException(
          ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to AVAILABLE from status: " + this.status
      );
    }
    this.status = ItemUnitStatus.AVAILABLE;
  }

  /**
   * 현재 유닛이 특정 Item 소속인지 확인한다.
   *
   * 주의:
   * - ItemUnit 자신은 반드시 유효한 Item을 참조해야 한다.
   * - targetItem이 null이면 false를 반환한다.
   */
  public boolean isBelongTo(Item targetItem) {
    validateLinkedItemExists();

    if (targetItem == null) {
      return false;
    }
    return this.item.getId().equals(targetItem.getId());
  }

  /**
   * 수정 화면에서의 유닛 삭제 가능 여부를 판단한다.
   * 이미 대여 중이거나 대여 요청 중인 유닛은 삭제할 수 없다.
   */
  public void validateDeletable() {
    validateStatusExists();

    if (this.status == ItemUnitStatus.RENTED || this.status == ItemUnitStatus.RENTAL_PENDING) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "대여 중이거나 대여 요청 중인 유닛은 삭제할 수 없습니다."
      );
    }
  }

  public boolean hasLabelIn(java.util.Set<String> labels) {
    return this.label != null && labels.contains(this.label);
  }

  public static ItemUnit create(Item item, String label) {
    if (item == null) {
      throw new DomainException(
          ErrorCode.INVALID_ITEM_UNIT,
          "Item must not be null."
      );
    }
    if (label == null || label.isBlank()) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Item unit label must not be blank."
      );
    }

    return ItemUnit.builder()
        .item(item)
        .label(label)
        .status(ItemUnitStatus.AVAILABLE)
        .build();
  }

  public void rename(String label) {
    if (label == null || label.isBlank()) {
      throw new DomainException(
          ErrorCode.BAD_REQUEST_EXCEPTION,
          "Item unit label의 값이 빈 값일 수 없습니다."
      );
    }
    this.label = label;
  }

  /**
   * ItemUnit의 상태가 존재하는지 검증한다.
   */
  private void validateStatusExists() {
    if (this.status == null) {
      throw new DomainException(
          ErrorCode.INVALID_ITEM_UNIT,
          "아이템 유닛에는 상태가 존재해야 합니다."
      );
    }
  }

  /**
   * ItemUnit이 유효한 Item을 참조하는지 검증한다.
   */
  private void validateLinkedItemExists() {
    if (this.item == null || this.item.getId() == null) {
      throw new DomainException(
          ErrorCode.INVALID_ITEM_UNIT,
          "아이템 유닛에는 연결된 아이템이 존재해야 합니다."
      );
    }
  }
}
