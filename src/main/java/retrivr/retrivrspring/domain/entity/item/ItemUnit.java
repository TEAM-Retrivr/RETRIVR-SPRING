package retrivr.retrivrspring.domain.entity.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item_unit", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"item_id", "code"})
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
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemUnitStatus status;

  public boolean isRentalAble() {
    return status.equals(ItemUnitStatus.AVAILABLE);
  }

  public void onRentalRequested() {
    transitionToRentalPendingStatus();
  }

  public void onRentalRejected() {
    transitionToAvailableStatus();
  }

  public void transitionToRentalPendingStatus() {
    if (!isRentalAble()) {
      throw new DomainException(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to RENTAL_PENDING from status: " + this.status);
    }
    this.status = ItemUnitStatus.RENTAL_PENDING;
  }

  public void transitionToAvailableStatus() {
    if (this.status != ItemUnitStatus.RENTAL_PENDING) {
      throw new DomainException(ErrorCode.ITEM_STATUS_TRANSITION_EXCEPTION,
          "Cannot transition to AVAILABLE from status: " + this.status);
    }
    this.status = ItemUnitStatus.AVAILABLE;
  }

  public boolean isBelongTo(Item targetItem) {
    if (item.getId() == null) {
      return false;
    }
    return this.item.getId().equals(targetItem.getId());
  }
}
