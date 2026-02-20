package retrivr.retrivrspring.domain.entity.rental;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "borrower")
public class Borrower extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "borrower_id")
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 255)
  private String phone;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "additional_borrower_info", columnDefinition = "jsonb")
  private JsonNode additionalBorrowerInfo;

  public static Borrower create(String name, String phone, JsonNode additionalBorrowerInfo) {
    return Borrower.builder()
        .name(name)
        .phone(phone)
        .additionalBorrowerInfo(additionalBorrowerInfo)
        .build();
  }
}
