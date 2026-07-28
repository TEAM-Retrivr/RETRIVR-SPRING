package retrivr.retrivrspring.application.service.admin.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordResetToken;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.PasswordResetTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.RefreshTokenRepository;
import retrivr.retrivrspring.domain.repository.auth.SignupTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.properties.EmailVerificationProperties;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyTokenResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    /**
     * 인증되지 않은 경로에서 요청할 수 있는 목적의 허용 목록.
     * 거부 목록이 아닌 허용 목록으로 두어, 목적이 추가될 때 기본적으로 비공개가 되도록 한다.
     */
    private static final Set<EmailVerificationPurpose> PUBLICLY_REQUESTABLE_PURPOSES = EnumSet.of(
            EmailVerificationPurpose.SIGNUP,
            EmailVerificationPurpose.PASSWORD_RESET
    );

    private final EmailVerificationRepository emailVerificationRepository;
    private final SignupTokenRepository signupTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeSender emailVerificationCodeSender;
    private final EmailVerificationProperties emailVerificationProperties;
    private final PasswordVerificationService passwordVerificationService;

    /**
     * 인증되지 않은 경로(public API)에서의 인증 코드 발송.
     * 미인증 요청이 허용되지 않는 목적(EMAIL_CHANGE)은 거부하여, 인증된 경로의 검증을 우회할 수 없게 한다.
     */
    public EmailVerificationSendResponse sendPublicCode(EmailVerificationSendRequest request) {
        assertPubliclyRequestable(request.purpose());
        return issueCode(request);
    }

    /**
     * 인증 코드를 발급·저장하고 메일로 발송한다.
     * 진입점별 권한/목적 검증을 마친 뒤에만 호출되어야 하므로 외부에 노출하지 않는다.
     */
    private EmailVerificationSendResponse issueCode(EmailVerificationSendRequest request) {
        String email = normalizeEmail(request.email());
        EmailVerificationPurpose purpose = request.purpose();

        LocalDateTime now = LocalDateTime.now();
        String rawCode = generateCode();
        String hashedCode = passwordEncoder.encode(rawCode);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .map(existing -> {
                    long remainingBlockSeconds = remainingResendBlockSeconds(existing, now);
                    if (remainingBlockSeconds > 0) {
                        // 남은 대기 시간을 함께 내려, 클라이언트가 상태를 잃어도 카운트다운을 복원할 수 있게 한다.
                        throw new ApplicationException(
                                ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS,
                                String.valueOf(remainingBlockSeconds)
                        );
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

        try {
            // (email, purpose) 유니크 제약. 동시 요청으로 같은 행이 먼저 생성되면 커밋이 아닌 이 시점에 드러나게 한다.
            emailVerificationRepository.saveAndFlush(verification);
        } catch (DataIntegrityViolationException e) {
            // 방금 다른 요청이 같은 목적의 코드를 발급했다는 뜻이므로, 재발송 차단과 동일하게 취급한다.
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
        }

        emailVerificationCodeSender.sendVerificationCode(
                email,
                rawCode,
                purpose,
                emailVerificationProperties.getExpiresSeconds()
        );
        return new EmailVerificationSendResponse(
                email,
                purpose.name(),
                emailVerificationProperties.getExpiresSeconds(),
                emailVerificationProperties.getResendBlockSeconds()
        );
    }

    /**
     * 이메일 변경용 인증 코드를 발송한다.
     * 발송 이전에 현재 이메일과의 동일 여부 및 타 단체 중복을 검증하여,
     * 검증 단계에서만 거절되어 쓸모없는 인증 코드가 발송되는 것을 방지한다.
     */
    public EmailVerificationSendResponse sendChangeEmailCode(
            AdminEmailVerificationSendRequest request,
            Long organizationId
    ) {
        passwordVerificationService.validate(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                request.passwordVerificationToken()
        );

        String email = normalizeEmail(request.email());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        organization.assertEmailChangeableTo(email);
        assertEmailNotUsedByOtherOrganization(email, organizationId);

        return issueCode(new EmailVerificationSendRequest(
                request.email(),
                EmailVerificationPurpose.EMAIL_CHANGE
        ));
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

        if (purpose == EmailVerificationPurpose.SIGNUP) {
            verification.markVerified(now);

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

            verification.markVerified(now);
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
     * refresh token 은 이메일로 단체를 식별하므로, 변경과 함께 기존 토큰을 모두 폐기하여 전 기기를 로그아웃시킨다.
     */
    @Transactional(noRollbackFor = ApplicationException.class)
    public void verifyChangeEmail(
            AdminEmailVerificationRequest request,
            Long organizationId
    ) {
        passwordVerificationService.validate(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                request.passwordVerificationToken()
        );

        String email = normalizeEmail(request.email());
        LocalDateTime now = LocalDateTime.now();

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        // 발송 시점에도 동일하게 검증하지만, 발송~검증 사이에 상태가 바뀔 수 있으므로 최종 방어선으로 다시 확인한다.
        // (코드 검증 이전에 확인하여, 인증 성공 후 실패로 인한 상태 불일치를 방지)
        organization.assertEmailChangeableTo(email);
        assertEmailNotUsedByOtherOrganization(email, organizationId);

        EmailVerification verification = verifyCodeOrThrow(
                email,
                EmailVerificationPurpose.EMAIL_CHANGE,
                request.code(),
                now
        );
        passwordVerificationService.validateAndConsume(
                organizationId,
                PasswordVerificationPurpose.EMAIL_CHANGE,
                request.passwordVerificationToken()
        );
        verification.markVerified(now);

        // 반드시 변경 전에 캡처한다. updateEmail 이후에는 새 이메일이 조회되어 엉뚱한 토큰을 지우게 된다.
        String previousEmail = organization.getEmail();
        refreshTokenRepository.deleteAllByEmail(previousEmail);

        organization.updateEmail(email);

        try {
            // organization.email 유니크 제약. 동시 변경 경합을 커밋 시점이 아닌 여기서 드러내어 400 으로 응답한다.
            organizationRepository.saveAndFlush(organization);
        } catch (DataIntegrityViolationException e) {
            // 이 메서드는 인증 실패 횟수 누적을 위해 noRollbackFor = ApplicationException 으로 선언되어 있다.
            // 제약 위반 이후의 영속성 컨텍스트는 커밋할 수 없으므로, 롤백 대상인 DomainException 으로 던져야 한다.
            throw new DomainException(ErrorCode.ALREADY_EXIST_EXCEPTION);
        }

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

    /**
     * 다른 단체가 이미 사용 중인 이메일인지 검증한다.
     * "이메일은 하나의 단체에만 속한다"는 불변식이지만 단일 엔티티가 답할 수 없으므로 서비스가 조율한다.
     * 커밋 시점의 유니크 제약 위반과 동일한 예외로 던져, 선검사와 경합 실패의 응답을 일치시킨다.
     */
    private void assertEmailNotUsedByOtherOrganization(String email, Long organizationId) {
        organizationRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(organizationId))
                .ifPresent(found -> {
                    throw new DomainException(ErrorCode.ALREADY_EXIST_EXCEPTION);
                });
    }

    /**
     * 인증되지 않은 진입점에서 허용되는 목적인지 검증한다.
     * purpose 는 경로가 아닌 요청 본문에서 오므로, 엔드포인트 분리만으로는 제한되지 않는다.
     */
    private void assertPubliclyRequestable(EmailVerificationPurpose purpose) {
        if (!PUBLICLY_REQUESTABLE_PURPOSES.contains(purpose)) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }
    }

    /**
     * 재발송이 가능해질 때까지 남은 시간(초). 0 이하이면 재발송 가능하다.
     * 마지막 발송 시각은 행의 updatedAt 이 기준이므로, 서버가 유일한 판단 주체다.
     */
    private long remainingResendBlockSeconds(EmailVerification verification, LocalDateTime now) {
        if (verification.getUpdatedAt() == null) {
            return 0;
        }

        long elapsedSeconds = Duration.between(verification.getUpdatedAt(), now).getSeconds();
        return emailVerificationProperties.getResendBlockSeconds() - elapsedSeconds;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}
