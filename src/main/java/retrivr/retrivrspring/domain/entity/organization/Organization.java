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

  @Column(name = "withdrawn_at")
  private LocalDateTime withdrawnAt;

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
          LocalDateTime withdrawnAt,
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
    this.withdrawnAt = withdrawnAt;
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

  public void changeName(String organizationName) {
    if (organizationName == null) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION);
    }

    String trimmedName = organizationName.trim();
    if (trimmedName.isEmpty() || trimmedName.length() > 255) {
      throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION);
    }
    this.name = trimmedName;
  }

  public void changeAdminCode(String encodedAdminCode) {
    this.adminAuthCode = AdminAuthCodeHash.fromHashed(
        requireHashedValue(encodedAdminCode, "encodedAdminCode")
    );
  }

  public void updateEmail(String email) {
    this.email = requireNonBlankEmail(email);
  }

  /**
   * 해당 이메일로 변경할 수 있는지 검증한다.
   * 현재 사용 중인 주소로는 인증 코드를 발송할 이유가 없으므로, 대소문자를 무시하고 동일 여부를 판정한다.
   */
  public void assertEmailChangeableTo(String newEmail) {
    String candidate = requireNonBlankEmail(newEmail).trim();
    if (this.email.trim().equalsIgnoreCase(candidate)) {
      throw new DomainException(ErrorCode.EMAIL_SAME_AS_CURRENT);
    }
  }

  public void updateProfileImageKey(String profileImageKey) {
    this.profileImageKey = (profileImageKey == null || profileImageKey.isBlank()) ? null : profileImageKey;
  }

  public void withdraw(LocalDateTime time) {
    if (this.status == OrganizationStatus.WITHDRAWN) {
      throw new DomainException(ErrorCode.ACCOUNT_WITHDRAWN);
    }
    this.status = OrganizationStatus.WITHDRAWN;
    this.withdrawnAt = time;
  }

  public String getPasswordHash() {
    return password == null ? null : password.getValue();
  }

  public String getAdminCodeHash() {
    return adminAuthCode == null ? null : adminAuthCode.getValue();
  }

  /**
   * 단체가 정상 운영 중인지 검증한다.
   * 탈퇴/정지/승인대기 단체는 운영 중이 아니며, 존재 여부가 드러나지 않도록 NOT_FOUND 로 응답한다.
   */
  public void assertOperating() {
    if (this.status != OrganizationStatus.ACTIVE) {
      throw new DomainException(ErrorCode.NOT_FOUND_ORGANIZATION);
    }
  }

  public void assertLoginAllowed() {
    if (this.status == OrganizationStatus.WITHDRAWN) {
      throw new DomainException(ErrorCode.ACCOUNT_WITHDRAWN);
    }

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
