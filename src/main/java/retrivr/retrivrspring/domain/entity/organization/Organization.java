package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "organization")
public class Organization extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "organization_id")
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Embedded
  private PasswordHash password;

  @Column(length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationStatus status;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "search_key", unique = true, length = 255)
  private String searchKey;

  @Embedded
  private AdminAuthCodeHash adminAuthCode;

  @Column(name = "profile_image_key", length = 500)
  private String profileImageKey;

  @Builder
  public Organization(
          Long id,
          String email,
          String passwordHash,
          String name,
          OrganizationStatus status,
          LocalDateTime lastLoginAt,
          String searchKey,
          String adminCodeHash,
          String profileImageKey
  ) {
    this.id = id;
    this.email = requireNonBlankEmail(email);
    this.password = PasswordHash.fromHashed(requireHashedValue(passwordHash, "passwordHash"));
    this.name = name;
    this.status = status;
    this.lastLoginAt = lastLoginAt;
    this.searchKey = searchKey;
    this.adminAuthCode = AdminAuthCodeHash.fromHashed(requireHashedValue(adminCodeHash, "adminCodeHash"));
    this.profileImageKey = profileImageKey;
  }

  public void updateLastLoginAt(LocalDateTime time) {
    this.lastLoginAt = time;
  }

  public void changePassword(String encodedPassword) {
    this.password = PasswordHash.fromHashed(requireHashedValue(encodedPassword, "encodedPassword"));
  }

  public void updateProfile(
          String email,
          String encodedPassword,
          String organizationName,
          String encodedAdminCode
  ) {
    this.email = requireNonBlankEmail(email);
    this.password = PasswordHash.fromHashed(requireHashedValue(encodedPassword, "encodedPassword"));
    this.name = organizationName;
    this.adminAuthCode = AdminAuthCodeHash.fromHashed(requireHashedValue(encodedAdminCode, "encodedAdminCode"));
  }

  public void updateProfileImageKey(String profileImageKey) {
    this.profileImageKey = profileImageKey;
  }

  public String getPasswordHash() {
    return password == null ? null : password.getValue();
  }

  public String getAdminCodeHash() {
    return adminAuthCode == null ? null : adminAuthCode.getValue();
  }

  public void assertLoginAllowed() {
    if (this.status == OrganizationStatus.SUSPENDED) {
      throw new DomainException(ErrorCode.ACCOUNT_SUSPENDED);
    }

    if (this.status != OrganizationStatus.ACTIVE) {
      throw new DomainException(ErrorCode.ACCOUNT_NOT_APPROVED);
    }
  }

  private String requireHashedValue(String value, String fieldName) {
    if (value == null) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION);
    }
    return value;
  }

  private String requireNonBlankEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION, "email must not be blank");
    }
    return email;
  }
}
