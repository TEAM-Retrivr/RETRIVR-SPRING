package retrivr.retrivrspring.entity.rental;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.item.Item;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rental_item")
public class RentalItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rental_item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rental_id", nullable = false)
  private Rental rental;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;
}
