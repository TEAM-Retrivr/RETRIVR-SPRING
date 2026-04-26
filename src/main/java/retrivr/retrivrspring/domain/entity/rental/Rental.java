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
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
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

  @Column(nullable = false, unique = true)
  private String publicId;

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

  @Column(name = "received_by")
  private String receivedBy;

  @Column(name = "returned_at")
  private LocalDateTime returnedAt;

  private RentalState state() {
    return switch (this.status) {
      case REQUESTED -> RequestedState.INSTANCE;
      case RENTED -> RentedState.INSTANCE;
      case RETURNED -> ReturnedState.INSTANCE;
      case REJECTED -> RejectedState.INSTANCE;
    };
  }

  public static Rental request(Item item, @Nullable ItemUnit itemUnit, Borrower borrower, String publicId) {
    Rental newRental = Rental.builder()
        .organization(item.getOrganization())
        .publicId(publicId)
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

  public void reject(String adminNameToReject, Organization organizationToReject) {
    state().reject(this, adminNameToReject, organizationToReject);
  }

  public void rejectBySystem(String systemMessage) {
    state().rejectBySystem(this, systemMessage);
  }

  public void changeDueDate(LocalDate newDueDate, Organization loginOrganization) {
    state().changeDueDate(this, newDueDate, loginOrganization);
  }

  public void markReturned(String adminNameToConfirm, Organization loginOrganization) {
    state().markReturned(this, adminNameToConfirm, loginOrganization);
  }

  public int getOverdueDays() {
    return state().getOverdueDays(this.dueDate, this.returnedAt);
  }

  public boolean canSendOverdueMessage() {
    return state().canSendOverdueMessage(this);
  }

  public int getRentalPeriod() {
    return state().getRentalPeriod(this.decidedAt, this.returnedAt, LocalDateTime.now());
  }
  /**
   *
   * 외부 사용 금지 메소드
   * State 패턴 클래스 전용 메소드
   *
   */
  protected void setRented(String admin, LocalDateTime now, LocalDate dueDate) {
    this.status = RentalStatus.RENTED;
    this.decidedBy = admin;
    this.decidedAt = now;
    this.dueDate = dueDate;
  }

  protected void setRejected(String admin, LocalDateTime now) {
    this.status = RentalStatus.REJECTED;
    this.decidedAt = now;
    this.decidedBy = admin;
  }

  protected void setReturned(String admin, LocalDateTime now) {
    this.status = RentalStatus.RETURNED;
    this.receivedBy = admin;
    this.returnedAt = now;
  }

  protected void setDueDateInternal(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  private void addItem(Item item) {
    this.rentalItems.add(RentalItem.create(this, item));
  }

  private void addItemUnit(ItemUnit itemUnit) {
    this.rentalItemUnits.add(RentalItemUnit.create(this, itemUnit));
  }

  /**
   *
   * 외부 사용 가능 메소드
   *
   */
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

  public boolean isOverdue() {
    return this.getOverdueDays() != 0;
  }

  public boolean isCountable() {
    return !(this.getStatus().equals(RentalStatus.REJECTED) || this.getStatus()
        .equals(RentalStatus.REQUESTED));
  }

  public boolean isRented() {
    return this.getStatus().equals(RentalStatus.RENTED);
  }
}
