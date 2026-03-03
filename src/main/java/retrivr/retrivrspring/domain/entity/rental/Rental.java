package retrivr.retrivrspring.domain.entity.rental;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.domain.entity.rental.state.RejectedState;
import retrivr.retrivrspring.domain.entity.rental.state.RentalState;
import retrivr.retrivrspring.domain.entity.rental.state.RentedState;
import retrivr.retrivrspring.domain.entity.rental.state.RequestedState;
import retrivr.retrivrspring.domain.entity.rental.state.ReturnedState;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rental")
public class Rental extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rental_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @OneToOne(fetch = FetchType.LAZY, optional = false,
      cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "borrower_id", nullable = false, unique = true)
  private Borrower borrower;

  @Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "rental",
      cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RentalItem> rentalItems = new ArrayList<>();

  @Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "rental",
      cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RentalItemUnit> rentalItemUnits = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RentalStatus status;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "decided_by")
  private String decidedBy;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "returned_at")
  private LocalDateTime returnedAt;

  private RentalState state() {
    return switch (this.status) {
      case REQUESTED -> RequestedState.INSTANCE;
      case APPROVED -> new RentedState();
      case RETURNED -> new ReturnedState();
      case REJECTED -> new RejectedState();
      default -> null;
    };
  }

  public static Rental request(Item item, @Nullable ItemUnit itemUnit, Borrower borrower) {
    Rental newRental = Rental.builder()
        .organization(item.getOrganization())
        .borrower(borrower)
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    item.onRentalRequested(itemUnit);
    newRental.addItem(item);

    if (itemUnit != null) {
      newRental.addItemUnit(itemUnit);
    }

    return newRental;
  }

  public void approve(String adminNameToApprove, Organization organizationToApprove) {
    state().approve(this, adminNameToApprove, organizationToApprove);
  }

  public void reject(String adminNameToReject, Organization organizationToApprove) {
    state().reject(this, adminNameToReject, organizationToApprove);
  }

  public void setRented(String admin, LocalDateTime now, LocalDate dueDate) {
    this.status = RentalStatus.APPROVED;
    this.decidedBy = admin;
    this.decidedAt = now;
    this.dueDate = dueDate;
  }

  public void setReject(String admin, LocalDateTime now) {
    this.status = RentalStatus.REJECTED;
    this.decidedAt = now;
    this.decidedBy = admin;
  }

  public boolean hasItemUnit() {
    return this.rentalItemUnits != null && !this.rentalItemUnits.isEmpty();
  }

  public Item getItem() {
    if (this.rentalItems == null || this.rentalItems.isEmpty()) {
      throw new DomainException(ErrorCode.INVALID_RENTAL_EXCEPTION, "대여정보에 아이템 내역이 없습니다.");
    }
    return this.rentalItems.getFirst().getItem();
  }

  public ItemUnit getItemUnit() {
    if (hasItemUnit()) {
      return this.rentalItemUnits.getFirst().getItemUnit();
    }
    return null;
  }

  public void validateRentalOwner(Organization organization) {
    if (!this.organization.getId().equals(organization.getId())) {
      throw new DomainException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
    }
  }

  public void changeDueDate(LocalDate newDueDate) {
    if (newDueDate == null) {
      throw new DomainException(ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION, "Cannot change due date to null");
    }
    if (!this.status.equals(RentalStatus.APPROVED) && !this.status.equals(RentalStatus.OVERDUE)) {
      throw new DomainException(ErrorCode.RENTAL_DUE_DATE_UPDATE_EXCEPTION, "Cannot change due date of rental that is not in APPROVED OR OVERDUE status");
    }
    this.dueDate = newDueDate;
    changeStatusByDeterminingOverDue();
  }

  public void changeStatusByDeterminingOverDue() {
    switch (this.status) {
      case APPROVED -> {
        if (this.dueDate.isBefore(LocalDate.now())) {
          this.status = RentalStatus.OVERDUE;
        }
      }
      case OVERDUE -> {
        if (this.dueDate.isAfter(LocalDate.now()) || this.dueDate.isEqual(LocalDate.now())) {
          this.status = RentalStatus.APPROVED;
        }
      }
    }
  }

  public void markReturned() {
    if (this.status == RentalStatus.RETURNED || this.returnedAt != null) {
      throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, "Already returned rental");
    }
    if (this.status != RentalStatus.APPROVED && this.status != RentalStatus.OVERDUE) {
      throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, "Cannot mark returned rental that is not in APPROVED OR OVERDUE status");
    }

    this.status = RentalStatus.RETURNED;
    this.returnedAt = LocalDateTime.now();
  }

  public int getOverdueDays() {
    // 배치 처리가 되지 않았을 수 있으니 모든 상태에서 처리 가능
    if (this.status == RentalStatus.APPROVED || this.status == RentalStatus.OVERDUE) {
      long days = ChronoUnit.DAYS.between(this.dueDate, LocalDate.now());
      return (int) Math.max(days, 0);
    }
    return 0;
  }

  public boolean canSendOverdueSms() {
    return borrower.getPhone() != null && !borrower.getPhone().isBlank();
  }

  public boolean isOverdue() {
    return this.dueDate != null && this.dueDate.isBefore(LocalDate.now());
  }

  private void addItem(Item item) {
    RentalItem newRentalItem = RentalItem.builder()
        .rental(this)
        .item(item)
        .build();

    this.rentalItems.add(newRentalItem);
  }

  private void addItemUnit(ItemUnit itemUnit) {
    RentalItemUnit newRentalItemUnit = RentalItemUnit.builder()
        .rental(this)
        .itemUnit(itemUnit)
        .build();

    this.rentalItemUnits.add(newRentalItemUnit);
  }
}
