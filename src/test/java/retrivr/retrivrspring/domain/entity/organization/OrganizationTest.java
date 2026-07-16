package retrivr.retrivrspring.domain.entity.organization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import retrivr.retrivrspring.domain.entity.organization.enumerate.OrganizationStatus;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationTest {

  private Organization organizationWith(OrganizationStatus status) {
    return Organization.builder()
        .id(1L)
        .email("org@retrivr.com")
        .passwordHash("hashed-password")
        .name("조직")
        .status(status)
        .adminCodeHash("hashed-code")
        .build();
  }

  @Test
  @DisplayName("ACTIVE 단체는 운영 중이다")
  void assertOperating_active_ok() {
    assertThatCode(() -> organizationWith(OrganizationStatus.ACTIVE).assertOperating())
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @EnumSource(value = OrganizationStatus.class, names = {"WITHDRAWN", "SUSPENDED", "PENDING"})
  @DisplayName("ACTIVE 가 아닌 단체는 존재 여부가 드러나지 않도록 NOT_FOUND 로 차단된다")
  void assertOperating_nonActive_throw(OrganizationStatus status) {
    assertThatThrownBy(() -> organizationWith(status).assertOperating())
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);
  }

  @Test
  @DisplayName("탈퇴 처리된 단체는 즉시 운영 중이 아니게 된다")
  void withdraw_thenNotOperating() {
    Organization organization = organizationWith(OrganizationStatus.ACTIVE);

    organization.withdraw(LocalDateTime.of(2026, 7, 16, 12, 0));

    assertThatThrownBy(organization::assertOperating)
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND_ORGANIZATION);
  }
}
