package retrivr.retrivrspring.application.service.admin.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.repository.auth.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.properties.EmailVerificationProperties;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

//todo email 인증을 redis 기반으로 바꿀 수 있을지도
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
        EmailVerificationPurpose purpose = request.purpose();

        LocalDateTime now = LocalDateTime.now();
        String rawCode = generateCode();
        String hashedCode = passwordEncoder.encode(rawCode);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .map(existing -> {
                    // 재전송 제한
                    if (existing.getUpdatedAt() != null && existing.getUpdatedAt().isAfter(now.minusSeconds(emailVerificationProperties.getResendBlockSeconds()))) {
                        throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
                    }

                    existing.refresh(hashedCode, now.plusSeconds(emailVerificationProperties.getExpiresSeconds()));
                    return existing;
                })
                .orElseGet(() ->
                        EmailVerification.create(
                                email,
                                purpose,
                                hashedCode,
                                now.plusSeconds(emailVerificationProperties.getExpiresSeconds())
                        )
                );

        emailVerificationRepository.save(verification);
        emailVerificationCodeSender.sendVerificationCode(email, rawCode, purpose, emailVerificationProperties.getExpiresSeconds());
        return new EmailVerificationSendResponse(email, purpose.name(), emailVerificationProperties.getExpiresSeconds());
    }

    @Transactional(noRollbackFor = ApplicationException.class)
    public Object verify(EmailVerificationRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        EmailVerificationPurpose purpose = request.purpose();
        String code = request.code();

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND)
                );

        LocalDateTime now = LocalDateTime.now();

        if (verification.isExpired(now)) {
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (verification.isVerified()) {
            throw new ApplicationException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (!passwordEncoder.matches(code, verification.getCode())) {
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

        verification.markVerified(now);

        // SIGNUP이면 signupToken 생성
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

            return EmailCodeVerifyTokenResponse.signupToken(rawSignupToken, emailVerificationProperties.getExpiresSeconds());
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

        if (purpose == EmailVerificationPurpose.EMAIL_CHANGE) {
            String rawEmailChangeToken = "ect_" + UUID.randomUUID();
            return EmailCodeVerifyTokenResponse.emailChangeToken(
                    rawEmailChangeToken,
                    emailVerificationProperties.getExpiresSeconds()
            );
        }

        // 그 외 purpose는 일반 인증 응답
        return new EmailVerificationResponse(
                email,
                true,
                verification.getVerifiedAt()
        );
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}
