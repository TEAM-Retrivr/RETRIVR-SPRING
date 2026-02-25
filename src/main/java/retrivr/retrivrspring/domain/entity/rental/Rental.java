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

    LocalDateTime now = LocalDateTime.now();
    //todo: 장바구니
    Item item = this.rentalItems.getFirst().getItem();
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
}
