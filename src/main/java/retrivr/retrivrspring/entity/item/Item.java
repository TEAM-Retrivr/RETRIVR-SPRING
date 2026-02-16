package retrivr.retrivrspring.entity.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.entity.BaseTimeEntity;
import retrivr.retrivrspring.entity.organization.Organization;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "item")
public class Item extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "rental_duration", nullable = false)
  private Integer rentalDuration;

  @Lob
  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;
}