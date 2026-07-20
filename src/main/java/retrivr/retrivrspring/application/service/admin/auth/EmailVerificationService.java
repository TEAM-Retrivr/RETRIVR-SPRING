package retrivr.retrivrspring.application.service.admin.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.properties.EmailVerificationProperties;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminEmailChangeResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final SignupTokenRepository signupTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeSender emailVerificationCodeSender;
    private final EmailVerificationProperties emailVerificationProperties;

    public EmailVerificationSendResponse sendCode(EmailVerificationSendRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String email = normalizeEmail(request.email());
        EmailVerificationPurpose purpose = request.purpose();

        LocalDateTime now = LocalDateTime.now();
        String rawCode = generateCode();
        String hashedCode = passwordEncoder.encode(rawCode);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .map(existing -> {
                    if (existing.getUpdatedAt() != null
                            && existing.getUpdatedAt().isAfter(now.minusSeconds(emailVerificationProperties.getResendBlockSeconds()))) {
                        throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
                    }

                    existing.refresh(hashedCode, now.plusSeconds(emailVerificationProperties.getExpiresSeconds()));
                    return existing;
                })
                .orElseGet(() -> EmailVerification.create(
                        email,
                        purpose,
                        hashedCode,
                        now.plusSeconds(emailVerificationProperties.getExpiresSeconds())
                ));

        switch (verification.getPurpose()) {
            case SIGNUP:
                signupTokenRepository.deleteByEmail(email);
                break;
            case PASSWORD_RESET:
                Organization organization = organizationRepository.findByEmail(email)
                        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
                passwordResetTokenRepository.deleteByOrganization(organization);
                break;
            case EMAIL_CHANGE:
                break;
        }

        emailVerificationRepository.save(verification);
        emailVerificationCodeSender.sendVerificationCode(
                email,
                rawCode,
                purpose,
                emailVerificationProperties.getExpiresSeconds()
        );
        return new EmailVerificationSendResponse(
                email,
                purpose.name(),
                emailVerificationProperties.getExpiresSeconds()
        );
    }

    public EmailVerificationSendResponse sendChangeEmailCode(EmailVerificationSendRequest request) {
        validateEmailChangePurpose(request.purpose());
        return sendCode(request);
    }

    @Transactional(noRollbackFor = ApplicationException.class)
    public EmailCodeVerifyTokenResponse verify(EmailVerificationRequest request) {
        // EMAIL_CHANGE 코드가 이 경로로 소비되면 인증 완료 처리만 되고 이메일은 변경되지 않은 채 코드가 소모된다.
        // 검증/상태 변경 이전에 거부한다.
        assertPubliclyRequestable(request.purpose());

        String email = normalizeEmail(request.email());
        EmailVerificationPurpose purpose = request.purpose();
        LocalDateTime now = LocalDateTime.now();

        EmailVerification verification = verifyCodeOrThrow(email, purpose, request.code(), now);
        verification.markVerified(now);

        if (purpose == EmailVerificationPurpose.SIGNUP) {
            String rawSignupToken = "st_" + UUID.randomUUID();
            String signupTokenHash = passwordEncoder.encode(rawSignupToken);

            signupTokenRepository.deleteByEmail(email);

            SignupToken token = SignupToken.builder()
                    .email(email)
                    .tokenHash(signupTokenHash)
                    .expiresAt(now.plusMinutes(10))
                    .build();

            token.markCodeVerified(now);
            signupTokenRepository.save(token);

            return EmailCodeVerifyTokenResponse.signupToken(
                    rawSignupToken,
                    emailVerificationProperties.getExpiresSeconds()
            );
        }

        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            Organization organization = organizationRepository.findByEmail(email)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND));

            passwordResetTokenRepository.deleteByOrganization(organization);

            String rawPasswordResetToken = "prt_" + UUID.randomUUID();
            String passwordResetTokenHash = passwordEncoder.encode(rawPasswordResetToken);

            PasswordResetToken token = PasswordResetToken.builder()
                    .organization(organization)
                    .tokenHash(passwordResetTokenHash)
                    .expiresAt(now.plusSeconds(emailVerificationProperties.getExpiresSeconds()))
                    .build();

            passwordResetTokenRepository.save(token);

            return EmailCodeVerifyTokenResponse.passwordResetToken(
                    rawPasswordResetToken,
                    emailVerificationProperties.getExpiresSeconds()
            );
        }

        throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
    }

    /**
     * 이메일 인증 코드를 검증하고, 성공 시 로그인한 단체의 이메일을 즉시 변경한다.
     * 별도의 인증 토큰을 발급하지 않고 인증 완료 시점에 이메일 변경을 반영한다.
     */
    @Transactional(noRollbackFor = ApplicationException.class)
    public AdminEmailChangeResponse verifyChangeEmail(EmailVerificationRequest request, Long organizationId) {
        validateEmailChangePurpose(request.purpose());

        String email = normalizeEmail(request.email());
        LocalDateTime now = LocalDateTime.now();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        // 다른 단체가 이미 사용 중인 이메일인지 검증 (코드 검증 이전에 확인하여, 인증 성공 후 실패로 인한 상태 불일치를 방지)
        organizationRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(organizationId))
                .ifPresent(found -> {
                    throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
                });

        EmailVerification verification = verifyCodeOrThrow(email, request.purpose(), request.code(), now);
        verification.markVerified(now);

        organization.updateEmail(email);

        return new AdminEmailChangeResponse(organization.getId(), organization.getEmail());
    }

    private EmailVerification verifyCodeOrThrow(String email, EmailVerificationPurpose purpose, String rawCode, LocalDateTime now) {
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.isExpired(now)) {
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (verification.isVerified()) {
            throw new ApplicationException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (!passwordEncoder.matches(rawCode, verification.getCode())) {
            int failedAttempts = verification.increaseFailedAttempts();
            if (failedAttempts >= emailVerificationProperties.getMaxFailedAttempts()) {
                verification.expire(now);
            }
            emailVerificationRepository.save(verification);

            if (failedAttempts >= emailVerificationProperties.getMaxFailedAttempts()) {
                throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
            }
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        return verification;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private void validateEmailChangePurpose(EmailVerificationPurpose purpose) {
        if (purpose != EmailVerificationPurpose.EMAIL_CHANGE) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }
    }
}
