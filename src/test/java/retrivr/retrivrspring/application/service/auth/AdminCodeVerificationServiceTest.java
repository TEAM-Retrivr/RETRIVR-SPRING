package retrivr.retrivrspring.application.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.application.service.admin.auth.AdminCodeVerificationService;
import retrivr.retrivrspring.domain.entity.organization.AdminCodeVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.AdminCodeVerificationTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminCodeVerificationRequest;

@ExtendWith(MockitoExtension.class)
class AdminCodeVerificationServiceTest {
  @Mock OrganizationRepository organizationRepository;
  @Mock RentalRepository rentalRepository;
  @Mock AdminCodeVerificationTokenRepository tokenRepository;
  @Mock PasswordEncoder passwordEncoder;
  @InjectMocks AdminCodeVerificationService service;

  private final Organization organization = mock(Organization.class);
  private final AdminCodeVerificationPurpose purpose = AdminCodeVerificationPurpose.ITEM_UPDATE;

  @Test
  void consumesLockedTokenAndRejectsReuse() {
    AdminCodeVerificationToken token = token(LocalDateTime.now());
    when(tokenRepository.findForUpdateByOrganizationAndPurpose(organization, purpose))
        .thenReturn(Optional.of(token));
    when(passwordEncoder.matches("raw", "hash")).thenReturn(true);

    service.validateAndConsumeAdminCodeVerificationToken(organization, purpose, "raw");
    assertThat(token.isUsed()).isTrue();
    assertThatThrownBy(() -> service.validateAndConsumeAdminCodeVerificationToken(organization, purpose, "raw"))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_USED_ADMIN_CODE_VERIFICATION_TOKEN);
    verify(tokenRepository, never()).findByOrganizationAndPurpose(any(), any());
  }

  @Test
  void expiredLockedTokenIsNotConsumed() {
    AdminCodeVerificationToken token = token(LocalDateTime.now().minusHours(2));
    when(tokenRepository.findForUpdateByOrganizationAndPurpose(organization, purpose))
        .thenReturn(Optional.of(token));
    when(passwordEncoder.matches("raw", "hash")).thenReturn(true);

    assertThatThrownBy(() -> service.validateAndConsumeAdminCodeVerificationToken(organization, purpose, "raw"))
        .isInstanceOf(ApplicationException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.EXPIRED_ADMIN_CODE_VERIFICATION_TOKEN);
    assertThat(token.isUsed()).isFalse();
  }

  @Test
  void refreshLocksExistingTokenAndInvalidatesOldHash() {
    AdminCodeVerificationToken token = token(LocalDateTime.now());
    token.markUsed(LocalDateTime.now());
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
    when(passwordEncoder.matches("123456", organization.getAdminCodeHash())).thenReturn(true);
    when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
    when(tokenRepository.findForUpdateByOrganizationAndPurpose(organization, purpose))
        .thenReturn(Optional.of(token));

    service.verifyAdminCode(1L, new AdminCodeVerificationRequest("123456", purpose));

    assertThat(token.getTokenHash()).isEqualTo("new-hash");
    assertThat(token.isUsed()).isFalse();
    verify(tokenRepository).save(token);
    verify(tokenRepository, never()).findByOrganizationAndPurpose(any(), any());
  }

  private AdminCodeVerificationToken token(LocalDateTime issuedAt) {
    return AdminCodeVerificationToken.create(organization, "hash", purpose, issuedAt);
  }
}
