package retrivr.retrivrspring.domain.entity.item;

import jakarta.persistence.*;
import lombok.*;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item_borrower_field")
public class ItemBorrowerField extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_borrower_field_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Column(nullable = false, length = 255)
  private String label;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static ItemBorrowerField of(
          Item item,
          String label,
          boolean required,
          int sortOrder
  ) {
    return ItemBorrowerField.builder()
            .item(item)
            .label(label)
            .isRequired(required)
            .sortOrder(sortOrder)
            .build();
  }
}
