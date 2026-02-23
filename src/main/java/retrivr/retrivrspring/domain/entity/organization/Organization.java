package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.*;
import lombok.*;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "organization")
public class Organization extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "organization_id")
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationStatus status;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "search_key", unique = true, length = 255)
  private String searchKey;

  public void updateLastLoginAt(LocalDateTime time) {
    this.lastLoginAt = time;
  }

  public void changePassword(String encodedPassword) {
    this.passwordHash = encodedPassword;
  }

}