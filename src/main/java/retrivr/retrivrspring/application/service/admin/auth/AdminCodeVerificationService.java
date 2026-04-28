package retrivr.retrivrspring.application.service.admin.auth;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.AdminCodeVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.enumerate.AdminCodeVerificationPurpose;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.auth.AdminCodeVerificationTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminCodeVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminCodeVerificationRequestForPublic;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminCodeVerificationResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminCodeVerificationResponseForPublic;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCodeVerificationService {

  private final OrganizationRepository organizationRepository;
  private final RentalRepository rentalRepository;
  private final AdminCodeVerificationTokenRepository adminCodeVerificationTokenRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AdminCodeVerificationResponse verifyAdminCode(Long loginOrganizationId, AdminCodeVerificationRequest request) {
    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
    
    // 관리자 코드가 일치하는지 검증
    if (!passwordEncoder.matches(request.adminCode(), organization.getAdminCodeHash())) {
      throw new ApplicationException(ErrorCode.ADMIN_CODE_MISMATCH);
    }

    // 새로운 토큰 값 생성
    String rawToken = "cvt_" + UUID.randomUUID();
    String encodedToken = passwordEncoder.encode(rawToken);
    LocalDateTime now = LocalDateTime.now();

    // 이미 토큰이 존재한다면 변경, 존재하지 않는다면 생성하여 저장
    AdminCodeVerificationToken token = adminCodeVerificationTokenRepository
        .findByOrganizationAndPurpose(organization, request.purpose())
        .map(existing -> {
          existing.refresh(encodedToken, now);
          return existing;
        })
        .orElseGet(() -> AdminCodeVerificationToken.create(
            organization,
            encodedToken,
            request.purpose(),
            now
        ));

    adminCodeVerificationTokenRepository.save(token);

    return new AdminCodeVerificationResponse(rawToken);
  }

  @Transactional
  public AdminCodeVerificationResponseForPublic verifyAdminCode(AdminCodeVerificationRequestForPublic request) {
    Rental rental = rentalRepository.findById(request.rentalId())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_RENTAL));

    Organization organization = rental.getOrganization();

    // 관리자 코드가 일치하는지 검증
    if (!passwordEncoder.matches(request.adminCode(), organization.getAdminCodeHash())) {
      throw new ApplicationException(ErrorCode.ADMIN_CODE_MISMATCH);
    }

    // 새로운 토큰 값 생성
    String rawToken = "cvt_" + UUID.randomUUID();
    String encodedToken = passwordEncoder.encode(rawToken);
    LocalDateTime now = LocalDateTime.now();

    // 이미 토큰이 존재한다면 변경, 존재하지 않는다면 생성하여 저장
    AdminCodeVerificationToken token = adminCodeVerificationTokenRepository
        .findByOrganizationAndPurpose(organization, request.purpose())
        .map(existing -> {
          existing.refresh(encodedToken, now);
          return existing;
        })
        .orElseGet(() -> AdminCodeVerificationToken.create(
            organization,
            encodedToken,
            request.purpose(),
            now
        ));

    adminCodeVerificationTokenRepository.save(token);

    return new AdminCodeVerificationResponseForPublic(rawToken, request.rentalId());
  }

  @Transactional
  public void validateAndConsumeAdminCodeVerificationToken(Organization organization, AdminCodeVerificationPurpose purpose, String rawToken) {
    LocalDateTime now = LocalDateTime.now();
    
    AdminCodeVerificationToken token = adminCodeVerificationTokenRepository.findByOrganizationAndPurpose(
            organization, purpose)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ADMIN_CODE_VERIFICATION_TOKEN));

    // 토큰 값의 일치 여부 검증
    if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
      throw new ApplicationException(ErrorCode.ADMIN_CODE_VERIFICATION_TOKEN_MISMATCH);
    }

    // 토큰 만료 여부 검증
    if (token.isExpired(now)) {
      throw new ApplicationException(ErrorCode.EXPIRED_ADMIN_CODE_VERIFICATION_TOKEN);
    }

    // 토큰 사용 여부 검증
    if (token.isUsed()) {
      throw new ApplicationException(ErrorCode.ALREADY_USED_ADMIN_CODE_VERIFICATION_TOKEN);
    }

    // 토큰 논리적 삭제
    token.markUsed(now);
  }

  public void validateAdminCodeVerificationToken(Organization organization, AdminCodeVerificationPurpose purpose, String rawToken) {
    LocalDateTime now = LocalDateTime.now();

    AdminCodeVerificationToken token = adminCodeVerificationTokenRepository.findByOrganizationAndPurpose(
            organization, purpose)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ADMIN_CODE_VERIFICATION_TOKEN));

    // 토큰 값의 일치 여부 검증
    if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
      throw new ApplicationException(ErrorCode.ADMIN_CODE_VERIFICATION_TOKEN_MISMATCH);
    }

    // 토큰 만료 여부 검증
    if (token.isExpired(now)) {
      throw new ApplicationException(ErrorCode.EXPIRED_ADMIN_CODE_VERIFICATION_TOKEN);
    }

    // 토큰 사용 여부 검증
    if (token.isUsed()) {
      throw new ApplicationException(ErrorCode.ALREADY_USED_ADMIN_CODE_VERIFICATION_TOKEN);
    }
  }
}
