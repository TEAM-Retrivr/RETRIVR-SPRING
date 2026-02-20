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

  //??
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by_organization_id")
  private Organization decidedByOrganization;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "returned_at")
  private LocalDateTime returnedAt;

  //??
  @Column(name = "return_date_override")
  private LocalDateTime returnDateOverride;

  public static Rental request(Organization organization, Item item, Borrower borrower) {
    Rental newRental = Rental.builder()
        .organization(organization)
        .borrower(borrower)
        .status(RentalStatus.REQUESTED)
        .requestedAt(LocalDateTime.now())
        .build();

    item.minusOneAvailableQuantity();

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

    itemUnit.transitionToRentalPendingStatus();

    newRental.rentalItemUnits.add(newRentalItemUnit);
    return newRental;
  }

}
