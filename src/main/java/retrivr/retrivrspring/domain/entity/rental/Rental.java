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
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.global.error.ApplicationException;
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

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "rental",
      cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RentalItem> rentalItems = new ArrayList<>();

  @Builder.Default
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

  public static Rental request(Organization organization, Item item, Borrower borrower) {
    Rental newRental = Rental.builder()
        .organization(organization)
        .borrower(borrower)
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    item.onRentalRequested();

    RentalItem newRentalItem = RentalItem.builder()
        .rental(newRental)
        .item(item)
        .build();

    newRental.rentalItems.add(newRentalItem);
    return newRental;
  }

  public static Rental request(Organization organization, Item item, ItemUnit itemUnit, Borrower borrower) {
    Rental newRental = request(organization, item, borrower);

    RentalItemUnit newRentalItemUnit = RentalItemUnit.builder()
        .rental(newRental)
        .itemUnit(itemUnit)
        .build();

    itemUnit.onRentalRequested();

    newRental.rentalItemUnits.add(newRentalItemUnit);
    return newRental;
  }

  public void approve(String adminNameToApprove, Organization organizationToApprove) {
    if (!this.status.equals(RentalStatus.REQUESTED)) {
      throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, "Cannot approve rental that is not in REQUESTED status");
    }
    if (!organizationToApprove.getId().equals(this.organization.getId())) {
      throw new DomainException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
    }

    if (this.rentalItems.isEmpty()) {
      throw new DomainException(ErrorCode.INVALID_RENTAL_EXCEPTION, "대여정보에 아이템 내역이 없습니다.");
    }
    //todo: 장바구니
    Item item = this.rentalItems.getFirst().getItem();

    if (this.hasItemUnit()) {
      ItemUnit itemUnit = this.rentalItemUnits.getFirst().getItemUnit();
      itemUnit.onRentalApprove();
    }

    LocalDateTime now = LocalDateTime.now();
    this.status = RentalStatus.APPROVED;
    this.decidedAt = now;
    this.decidedBy = adminNameToApprove;
    this.dueDate = now.plusDays(item.getRentalDuration()).toLocalDate();
  }

  public void reject(String adminNameToReject, Organization organizationToApprove) {
    if (!this.status.equals(RentalStatus.REQUESTED)) {
      throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, "Cannot reject rental that is not in REQUESTED status");
    }
    if (!organizationToApprove.getId().equals(this.organization.getId())) {
      throw new DomainException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
    }

    if (this.rentalItems.isEmpty()) {
      throw new DomainException(ErrorCode.INVALID_RENTAL_EXCEPTION, "대여정보에 아이템 내역이 없습니다.");
    }
    //todo: 장바구니
    Item item = this.rentalItems.getFirst().getItem();
    item.onRentalRejected();

    if (!rentalItemUnits.isEmpty()) {
      ItemUnit itemUnit = this.rentalItemUnits.getFirst().getItemUnit();
      itemUnit.onRentalRejected();
    }

    this.status = RentalStatus.REJECTED;
    this.decidedAt = LocalDateTime.now();
    this.decidedBy = adminNameToReject;
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
        if (this.dueDate.isAfter(LocalDate.now())) {
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
    if (this.status != RentalStatus.OVERDUE) {
      throw new DomainException(ErrorCode.RENTAL_STATUS_TRANSITION_EXCEPTION, "Cannot get overdue days of rental that is not in OVERDUE status");
    }

    long days = ChronoUnit.DAYS.between(this.dueDate, LocalDate.now());
    return (int) Math.max(days, 0);
  }

  public boolean canSendOverdueSms() {
    return borrower.getPhone() != null && !borrower.getPhone().isBlank();
  }

  public boolean isOverdue() {
    long days = ChronoUnit.DAYS.between(this.dueDate, LocalDate.now());
    return days != 0;
  }
}
